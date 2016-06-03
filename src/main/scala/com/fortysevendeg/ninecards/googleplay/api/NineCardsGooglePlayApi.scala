package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.Monad
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.{ Http4sGooglePlayApiClient, Http4sGooglePlayWebScraper, TaskInterpreter }
import scalaz.concurrent.Task
import io.circe.generic.auto._
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing._

class NineCardsGooglePlayActor extends Actor with HttpService {

  import TaskInterpreter._

  def actorRefFactory = context

  // todo make a new wiring module
  implicit val inter: GooglePlayOps ~> Task = {
    val apiClient = new Http4sGooglePlayApiClient(  getConfigValue("googleplay.api.endpoint"))
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint"))
    interpreter(apiClient.request _, webClient.request _)
  }

  private val api = new NineCardsGooglePlayApi()

  def receive = runRoute(api.googlePlayApiRoute[Task])
}

class NineCardsGooglePlayApi() {

  import Directives._
  import CustomDirectives._
  import com.fortysevendeg.ninecards.googleplay.ninecardsspray._

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
