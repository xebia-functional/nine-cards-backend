package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.Monad
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import io.circe.generic.auto._
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing.{Directives, HttpService, Route}

class NineCardsGooglePlayActor( googlePlayInterpreter: GooglePlayOps ~> Task) extends Actor with HttpService {

  override def actorRefFactory = context

  private val googleApiRoute: Route = {
    import NineCardsMarshallers._
    implicit val inter: GooglePlayOps ~> Task = googlePlayInterpreter
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
