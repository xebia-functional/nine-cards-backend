package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.Monad
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import io.circe.generic.auto._
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing.{Directives, HttpService, Route}

class NineCardsGooglePlayActor(interpreter: GooglePlay.Ops ~> Task) extends Actor with HttpService {

  override def actorRefFactory = context

  private val apiRoute: Route = {
    import NineCardsMarshallers._
    implicit val inter: GooglePlay.Ops ~> Task = interpreter
    NineCardsGooglePlayApi.googlePlayApiRoute[Task]
  }

  def receive = runRoute(apiRoute)

}

object NineCardsGooglePlayApi {

  import CustomDirectives._
  import Directives._
  import NineCardsMarshallers._

  def googlePlayApiRoute[M[_]](
    implicit
      monadM: Monad[M],
      googlePlayService: GooglePlay.Service[GooglePlay.Ops],
      interpreter: GooglePlay.Ops ~> M, // todo can this be made GPO ~> TRM
      itemMarshaller: ToResponseMarshaller[M[Option[Item]]], // todo need to make the option[item] generic
      bulkMarshaller: ToResponseMarshaller[M[PackageDetails]]
  ): Route =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localizationOption) =>
        get {
          path("package" / Segment) { packageName =>
            complete {
              googlePlayService.resolve(GoogleAuthParams(androidId, token, localizationOption), Package(packageName)).foldMap(interpreter)
            }
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageList]) { req =>
              complete {
                googlePlayService.resolveMany(GoogleAuthParams(androidId, token, localizationOption), req).foldMap(interpreter)
              }
            }
          }
        }
      }
    }
}
