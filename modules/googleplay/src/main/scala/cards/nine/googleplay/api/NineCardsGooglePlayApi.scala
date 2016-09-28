package cards.nine.googleplay.api

import akka.actor.Actor
import cats.~>
import cats.data.Xor
import cats.free.Free
import cards.nine.googleplay.extracats._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.Wiring
import cards.nine.googleplay.service.free.algebra.GooglePlay
import io.circe.generic.auto._
import io.circe.spray.JsonSupport
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
        recommendation.route
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

  object recommendation {

    import CirceCoders._
    import JsonSupport._

    lazy val route: Route =
      pathPrefix("recommendations") {
        requestHeaders { authParams =>
          pathEndOrSingleSlash {
            post {
              entity(as[ApiRecommendByAppsRequest])  { request =>
                complete( recommendByAppList(authParams, request) )
              }
            }
          } ~
          pathPrefix(CategorySegment) { category =>
            priceFilterPath { filter =>
              (post & entity(as[ApiRecommendByCategoryRequest]) ) { request =>
                complete ( recommendByCategory(authParams, category, filter, request) )
              }
            }
          }
        }
      }

    private[this] def recommendByCategory(
      authParams: GoogleAuthParams,
      category: Category,
      filter: PriceFilter,
      apiRequest: ApiRecommendByCategoryRequest
    ): Free[Ops, Xor[InfoError, ApiRecommendationList]] = {
      val request = Converters.toRecommendByCategoryRequest(category, filter, apiRequest)
      googlePlayService
        .recommendationsByCategory(authParams, request)
        .map(_.map(Converters.toApiRecommendationList))
    }

    private[this] def recommendByAppList(
      authParams: GoogleAuthParams, apiRequest: ApiRecommendByAppsRequest
    ) : Free[Ops, ApiRecommendationList] = {
      val request = Converters.toRecommendByAppsRequest(apiRequest)
      googlePlayService
        .recommendationsByAppList(authParams, request)
        .map( Converters.toApiRecommendationList)
    }
  }

}
