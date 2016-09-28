package cards.nine.googleplay.api

import cats.syntax.xor._
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.extracats._
import cards.nine.googleplay.processes.Wiring
import cards.nine.googleplay.service.free.algebra.GooglePlay
import io.circe.parser._
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import scala.concurrent.duration._
import scalaz.concurrent.Task
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpRequest
import spray.http.StatusCodes._
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

class ApiIntegration extends Specification with Specs2RouteTest with HttpService with AfterAll {

  def actorRefFactory = system // connect the DSL to the test ActorSystem

  /*
   * Reasons this test suite may fail:
   *   * The X-Android-ID and X-Google-Play-Token parameters are no longer accepted by Google Play.
   *   * The packages in the field validPackages are no longer in the Google Play Store (Spain, set by the X-Android-Market-Localization header)
   *       - The URL for checking the validity is https://play.google.com/store/apps/details?id=$PACKAGE&hl=es_ES
   */

  import NineCardsMarshallers._
  import AuthHeadersRejectionHandler._

  implicit val defaultTimeout = RouteTestTimeout(20.seconds)

  val requestHeaders = List(
    RawHeader(Headers.androidId, androidId.value),
    RawHeader(Headers.token, token.value),
    RawHeader(Headers.localization, localization.value)
  )

  import CirceCoders._

  private[this] val wiring = new Wiring()

  implicit val i = wiring.interpreter

  override def afterAll = wiring.shutdown

  val route = {
    val trmFactory: TRMFactory[ GooglePlay.FreeOps ] =
      NineCardsMarshallers.contraNaturalTransformFreeTRMFactory[GooglePlay.Ops, Task](
        i, taskMonad, NineCardsMarshallers.TaskMarshallerFactory)

    val api = new NineCardsGooglePlayApi[GooglePlay.Ops]()(GooglePlay.Service.service, trmFactory)
    sealRoute(api.googlePlayApiRoute )
  }

  val validPackages = List("air.fisherprice.com.shapesAndColors", "com.rockstargames.gtalcs", "com.ted.android")
  val invalidPackages = List("com.package.does.not.exist", "com.another.invalid.package")
  val allPackages = validPackages ++ invalidPackages

  sequential

  def failUnauthorized(req: HttpRequest) = {
    "Fail with a 401 Unauthorized if the needed headers are not given" in {
      req ~> route ~> check {
        status must_=== Unauthorized
      }
    }
  }

  def failMethodNotAllowed(req: HttpRequest) = {
    "Fail with a 405 MethodNotAllowed if the wrong HTTP method is used" in {
      req ~> route ~> check {
        status must_=== MethodNotAllowed
      }
    }
  }

  endpoints.item should {

    failUnauthorized( Get(s"/googleplay/package/${validPackages.head}") )

    "Successfully connect to Google Play and obtain an item for a single package" in {
      val name = validPackages.head
      Get(s"/googleplay/package/${name}") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK
        val entity = decode[Item](responseAs[String])
        entity.map(_.docV2.docid) must_=== validPackages.head.right
      }
    }

    "Fail to obtain an item for an incorrect package" in {
      val name = invalidPackages.head
      Get(s"/googleplay/package/$name") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== InternalServerError
        responseAs[String] must_=== "Cannot find item!"
      }
    }
  }

  endpoints.itemList should {

    val request = Post(s"/googleplay/packages/detailed", PackageList(allPackages))

    failUnauthorized(request)

    "Successfully connect to Google Play and give a series of items for a list of packages" in {
      request ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val response = decode[PackageDetails](responseAs[String]).map {
          case PackageDetails(errors, items) => (errors.toSet, items.map(_.docV2.docid).toSet)
        }

        response must_=== (invalidPackages.toSet, validPackages.toSet).right
      }
    }

  }

  endpoints.card should {

    failUnauthorized(Get(s"/googleplay/cards/{validPackages.head}"))

    "Successfully connect to Google Play and obtain an card for a single package" in {
      val packageName = validPackages.head
      Get(s"/googleplay/cards/$packageName") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK
        val docid = decode[ApiCard](responseAs[String]).map(_.packageName)
        docid must_=== packageName.right
      }
    }

    "Fail to obtain an item for an incorrect package" in {
      val packageName = invalidPackages.head
      Get(s"/googleplay/cards/$packageName") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== NotFound
        val err = decode[InfoError](responseAs[String])
        err.map(_.message) must_=== packageName.right
      }
    }

  }

  endpoints.cardList should {

    val request = Post(s"/googleplay/cards", PackageList(allPackages))

    failUnauthorized(request)

    "Successfully connect to Google Play and give a series of cards for a list of packages" in {
      request ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK

        val sets = decode[ApiCardList](responseAs[String]).map {
          case ApiCardList(errors, apps) => (errors.toSet, apps.map(_.packageName).toSet)
        }

        sets must_=== (invalidPackages.toSet, validPackages.toSet).right
      }
    }
  }

  endpoints.recommendCategory should {

    val apiRequest = ApiRecommendByCategoryRequest( excludedApps = List(), maxTotal = 10)
    val uri = "/googleplay/recommendations/SOCIAL/FREE"

    val request = Post("/googleplay/recommendations/SOCIAL/FREE", apiRequest)

    failUnauthorized(request)

    "Successfully connect to Google Play and give the information for recommended apps" in {
      request ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK
        decode[ApiRecommendationList](responseAs[String]).map(_.apps).toEither must beRight
      }
    }
  }

  endpoints.recommendAppList should {

    val apiRequest =  ApiRecommendByAppsRequest(
      searchByApps = allPackages.map(Package.apply),
      numPerApp = 3, 
      excludedApps = List(), 
      maxTotal = 10
    )

    val request = Post("/googleplay/recommendations", apiRequest)

    failUnauthorized(request)

    "Successfully connect to Google Play" in {
      request ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== OK
      }
    }

  }

}
