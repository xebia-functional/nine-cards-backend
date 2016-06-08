package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.Monad
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.{ Http4sGooglePlayApiClient, Http4sGooglePlayWebScraper, TaskInterpreter }
import io.circe.generic.auto._
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing.{Directives, HttpService, Route}

class NineCardsGooglePlayActor extends Actor with HttpService {

  override def actorRefFactory = context

  // todo make a new wiring module

  private val googleApiRoute: Route = {
    import NineCardsMarshallers._

    // todo make a new wiring module
    implicit val inter: GooglePlayOps ~> Task = {
      val client = PooledHttp1Client()
      val apiClient = new Http4sGooglePlayApiClient(  getConfigValue("googleplay.api.endpoint"), client)
      val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint"), client)
      TaskInterpreter(apiClient, webClient)
    }

    NineCardsGooglePlayApi.googlePlayApiRoute[Task]
  }

  def receive = runRoute(googleApiRoute)

}

object NineCardsGooglePlayApi {

  import CustomDirectives._
  import Directives._
  import NineCardsMarshallers._

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
