package com.fortysevendeg.ninecards.api

import cats.data.Xor
import com.fortysevendeg.ninecards.api.messages.GooglePlayMessages._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.rankings
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.CategorizedApp
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.services.free.domain.Category
import enumeratum.{ Enum, EnumEntry, Circe ⇒ CirceEnum }
import io.circe.{ Decoder, Encoder, Json }
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import spray.httpx.SprayJsonSupport
import spray.json._

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  object JodaDateTimeFormat extends RootJsonFormat[DateTime] {
    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val dateExample = formatter.print(0)

    def error(v: String) = deserializationError(
      s"'$v' is not a valid date value. The format for dates must be: '$dateExample'"
    )

    val decodeDateTime: Decoder[DateTime] = Decoder.instance { cursor ⇒
      cursor.as[String].flatMap {
        dateTime ⇒ Xor.right(DateTime.parse(dateTime, formatter))
      }
    }

    val encodeDateTime: Encoder[DateTime] = Encoder.instance { dateTime: DateTime ⇒
      Json.fromString(formatter.print(dateTime))
    }

    def write(obj: DateTime): JsValue = JsString(encodeDateTime(obj).noSpaces)

    def read(json: JsValue): DateTime = json match {
      case JsString(s) ⇒ decodeDateTime(Json.fromString(s).hcursor).fold(error(s), d ⇒ d)
      case _ ⇒ error(json.toString)
    }

  }

  def jodaDateTimeFormat(formatter: DateTimeFormatter): RootJsonFormat[DateTime] = {

    val dateExample = formatter.print(0)
    def toError(v: String): Nothing = deserializationError(
      s"'$v' is not a valid date value. The format for dates must be: '$dateExample'"
    )

    val decodeDateTime: Decoder[DateTime] = Decoder.instance { cursor ⇒
      cursor.as[String].flatMap { str ⇒
        Xor.right(DateTime.parse("2016-01-01", formatter))
      }
    }

    val encodeDateTime: Encoder[DateTime] = Encoder.instance { dateTime: DateTime ⇒
      Json.fromString(formatter.print(dateTime))
    }
    new CirceOnJsString[DateTime](encodeDateTime, decodeDateTime, toError)
  }

  class CirceOnJsString[T](encoder: Encoder[T], decoder: Decoder[T], toError: String ⇒ Nothing)
    extends RootJsonFormat[T] {

    def cutQuotes(s: String) = s.substring(1, s.length - 1)

    def write(obj: T): JsValue = {
      val s = encoder(obj).noSpaces.toString
      JsString(cutQuotes(encoder(obj).noSpaces))
    }

    def read(jsVal: JsValue): T = jsVal match {
      case JsString(s) ⇒
        val j = Json.fromString(s)
        decoder.decodeJson(j).fold(toError(jsVal.toString), d ⇒ d)
      case _ ⇒ toError(jsVal.toString)
    }

  }

  def enumJsonFormat[E <: EnumEntry](implicit en: Enum[E]): JsonFormat[E] = {
    def toError(v: String): Nothing = {
      val validVals = en.values.mkString(",")
      val message = s"$v is not a valid value. Allowed values are [$validVals]"
      deserializationError(message)
    }
    new CirceOnJsString[E](CirceEnum.encoder(en), CirceEnum.decoder(en), toError)
  }

  implicit val category = enumJsonFormat[Category]

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat2(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)

  implicit val appInfoFormat = jsonFormat7(AppInfo)

  implicit val apiSharedCollection = {
    implicit val dateTimeFormat: RootJsonFormat[DateTime] = JodaDateTimeFormat
    jsonFormat12(ApiSharedCollection)
  }

  implicit val apiSharedCollectionList = jsonFormat1(ApiSharedCollectionList)

  implicit val apiCreateCollectionRequestFormat = jsonFormat9(ApiCreateCollectionRequest)

  implicit val packagesStatsFormat = jsonFormat2(PackagesStats)

  implicit val apiCreateCollectionResponseFormat = jsonFormat2(ApiCreateOrUpdateCollectionResponse)

  implicit val apiSubscribeResponseFormat = jsonFormat0(ApiSubscribeResponse)

  implicit val apiUnsubscribeResponseFormat = jsonFormat0(ApiUnsubscribeResponse)

  implicit val sharedCollectionUpdateInfoFormat = jsonFormat2(SharedCollectionUpdateInfo)

  implicit val apiUpdateCollectionRequestFormat = jsonFormat2(ApiUpdateCollectionRequest)

  implicit val apiCategorizeAppsRequestFormat = jsonFormat1(ApiCategorizeAppsRequest)

  implicit val categorizedAppFormat = jsonFormat2(CategorizedApp)

  implicit val apiCategorizeAppsResponseFormat = jsonFormat2(ApiCategorizeAppsResponse)

  implicit val apiCatRanking = jsonFormat2(rankings.CategoryRanking)

  implicit val apiRanking = jsonFormat1(rankings.Ranking)

  implicit val apiReloadRankingRequest = {
    val format = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC
    implicit val dateTime: JsonFormat[DateTime] = jodaDateTimeFormat(format)
    jsonFormat1(rankings.Reload.Request)
  }

  implicit val apiReloadRankingResponse = jsonFormat0(rankings.Reload.Response)

}

object JsonFormats extends JsonFormats