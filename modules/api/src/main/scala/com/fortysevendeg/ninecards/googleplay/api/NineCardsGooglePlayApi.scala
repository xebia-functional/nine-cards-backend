package com.fortysevendeg.ninecards.api

import spray.http.{ContentTypes, HttpEntity, HttpResponse}
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.routing._
import akka.actor.Actor
import com.akdeniz.googleplaycrawler.GooglePlayAPI
import org.apache.http.impl.client.DefaultHttpClient
import scala.collection.JavaConversions._
import shapeless._
import io.circe._

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute)
}

object NineCardsGooglePlayApi {
  implicit def categoryMarshaller: Marshaller[CategoryValues] = Marshaller.of[CategoryValues](ContentTypes.`application/json`) {
    case (CategoryValues(cs), contentType, ctx) =>
      val asJson =
        Json.obj("docV2" ->
          Json.obj("details" ->
            Json.obj("appDetails" ->
              Json.obj("appCategory" ->
                Json.array(cs.map(Json.string): _*)))))

      ctx.marshalTo(HttpEntity(ContentTypes.`application/json`, asJson.noSpaces))
  }
}

trait NineCardsGooglePlayApi extends HttpService {
  import NineCardsGooglePlayApi._

  def googlePlayApiRoute: Route = packageRoute

  type Headers = Token :: AndroidId :: Option[Localisation] :: HNil

  val requestHeaders: Directive[Headers] = for {
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localisation.apply) :: HNil

  private[this] def packageRoute =
    get {
      path("googleplay" / "package" / Segment) { packageName =>
        requestHeaders { case (Token(token), AndroidId(androidId), localisationOption) =>
          val gpApi = new GooglePlayAPI()
          gpApi.setToken(token)
          gpApi.setAndroidID(androidId)
          gpApi.setClient(new DefaultHttpClient)
          localisationOption.foreach(l => gpApi.setLocalization(l.value))

          val categoryList = gpApi.details(packageName).getDocV2.getDetails.getAppDetails.getAppCategoryList.toList

          complete(CategoryValues(categoryList))
        }
      }
    }
}

case class Token(value: String) extends AnyVal
case class AndroidId(value: String) extends AnyVal
case class Localisation(value: String) extends AnyVal
case class CategoryValues(value: List[String]) extends AnyVal
