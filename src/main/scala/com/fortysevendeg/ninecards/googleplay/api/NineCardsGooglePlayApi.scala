package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.Actor
import cats.~>
import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.processes.Wiring
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import io.circe.generic.auto._
import scalaz.concurrent.Task
import spray.routing.{Directives, HttpService, Route}
import NineCardsMarshallers.TRMFactory

class NineCardsGooglePlayActor extends Actor with HttpService {

  override def actorRefFactory = context

  private val apiRoute: Route = {

    val interpreter: GooglePlay.Ops ~> Task = new Wiring().interpreter
    implicit val trmFactory: TRMFactory[ GooglePlay.FreeOps ] =
      NineCardsMarshallers.contraNaturalTransformFreeTRMFactory[GooglePlay.Ops, Task](
        interpreter, taskMonad, NineCardsMarshallers.TaskMarshallerFactory)
    new NineCardsGooglePlayApi[GooglePlay.Ops]().googlePlayApiRoute
  }

  import AuthHeadersRejectionHandler._

  def receive = runRoute(apiRoute)

}

class NineCardsGooglePlayApi[Ops[_]] (
  implicit
  googlePlayService: GooglePlay.Service[Ops],
  marshallerFactory: TRMFactory[Free[Ops, ?]]
){
  import CustomDirectives._
  import CustomMatchers._
  import Directives._
  import NineCardsMarshallers._
  import marshallerFactory._

  val googlePlayApiRoute: Route =
    pathPrefix("googleplay") {
      requestHeaders { _auth =>
        cardsRoute ~
        packageRoute ~
        packagesRoute ~
        recommendationsRoute
      }
    }

  private[this] lazy val packageRoute: Route =
    pathPrefix("package") {
      requestHeaders { authParams =>
        path(Segment) { packageName =>
          get {
            complete ( googlePlayService.resolve( authParams, Package(packageName)) )
          }
        }
      }
    }

  private[this] lazy val packagesRoute: Route =
    pathPrefix("packages" / "detailed" ) {
      post {
        requestHeaders { authParams =>
          entity(as[PackageList]){ req =>
            complete ( googlePlayService.resolveMany(authParams, req) )
          }
        }
      }
    }

  private[this] lazy val cardsRoute: Route =
    pathPrefix("cards") {
      requestHeaders { authParams =>
        pathEndOrSingleSlash {
          post {
            entity(as[PackageList]) { packageList =>
              complete ( getCardList( authParams, packageList) )
            }
          }
        } ~
        pathPrefix(Segment) { packageName =>
          get {
            complete( getCard(authParams, packageName) )
          }
        }
      }
    }

  private[this] lazy val recommendationsRoute: Route =
    pathPrefix("recommendations") {
      requestHeaders { authParams =>
        pathEndOrSingleSlash {
          post {
            entity(as[PackageList])  { packageList =>
              complete ( recommendByAppList(authParams, packageList) )
            }
          }
        } ~
        pathPrefix(CategorySegment) { category =>
          priceFilterPath { filter =>
            get {
              complete ( recommendByCategory(authParams, category, filter) )
            }
          }
        }
      }
    }

  private[this] def getCard(
    authParams: GoogleAuthParams, packageName: String
  ): Free[Ops, Xor[InfoError, ApiCard]] =
    googlePlayService
      .getCard( authParams, Package(packageName))
      .map(_.map(Converters.toApiCard))

  private[this] def getCardList(
    authParams: GoogleAuthParams, packageList: PackageList
  ): Free[Ops, ApiCardList] =
    googlePlayService
      .getCardList( authParams, packageList)
      .map(Converters.toApiCardList)

  private[this] def recommendByCategory(
    authParams: GoogleAuthParams, category: Category, filter: PriceFilter
  ): Free[Ops, Xor[InfoError, ApiRecommendationList]] =
    googlePlayService
      .recommendationsByCategory(authParams, category, filter)
      .map(_.map(Converters.toApiRecommendationList))

  private[this] def recommendByAppList(
    authParams: GoogleAuthParams, packages: PackageList
  ) : Free[Ops, ApiRecommendationList] =
    googlePlayService
      .recommendationsByAppList(authParams, packages)
      .map( Converters.toApiRecommendationList)

}
