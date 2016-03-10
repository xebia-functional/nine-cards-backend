package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.config.NineCardsConfig
import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.extracats._
import cats.Monad
import cats.~>
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.{ Http4sGooglePlayApiClient, Http4sGooglePlayWebScraper }
import spray.routing._
import spray.httpx.marshalling.ToResponseMarshaller
import akka.actor.Actor
import shapeless._
import scalaz.concurrent.Task
import io.circe.generic.auto._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.TaskInterpreter

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  import TaskInterpreter._

  def actorRefFactory = context

  // todo make a new wiring module
  val apiEndpoint = NineCardsConfig.getConfigValue("googleplay.api.endpoint")
  val apiClient = new Http4sGooglePlayApiClient(apiEndpoint)
  val webEndpoint = NineCardsConfig.getConfigValue("googleplay.web.endpoint")
  val webClient = new Http4sGooglePlayWebScraper(webEndpoint)

  implicit val i = interpreter(apiClient.request _, webClient.request _)

  def receive = runRoute(googlePlayApiRoute[Task])
}

trait NineCardsGooglePlayApi extends HttpService {

  val requestHeaders = for { // todo put this in a package object somewhere
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

  def googlePlayApiRoute[M[_]](
    implicit
      monadM: Monad[M],
      googlePlayService: GooglePlayService[GooglePlayOps],
      interpreter: GooglePlayOps ~> M, // todo can this be made GPO ~> TRM
      itemMarshaller: ToResponseMarshaller[M[Option[Item]]], // todo need to make the option[item] generic
      bulkMarshaller: ToResponseMarshaller[M[PackageDetails]]
  ): Route =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localizationOption) =>
        get {
          path("package" / Segment) { packageName =>
            complete {
              googlePlayService.requestPackage((token, androidId, localizationOption), Package(packageName)).foldMap(interpreter)
            }
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageListRequest]) { req =>
              complete {
                googlePlayService.bulkRequestPackage((token, androidId, localizationOption), req).foldMap(interpreter)
              }
            }
          }
        }
      }
    }
}
