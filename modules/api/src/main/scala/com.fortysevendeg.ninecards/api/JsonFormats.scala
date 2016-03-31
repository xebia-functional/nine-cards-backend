package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.domain._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.util.{Success, Try}

trait JsonFormats
  extends DefaultJsonProtocol
    with SprayJsonSupport {

  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

    val formatter = ISODateTimeFormat.basicDateTime

    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): DateTime = json match {
      case JsString(s) =>
        Try(formatter.parseDateTime(s)) match {
          case Success(d) => d
          case _ => error(s)
        }
      case _ =>
        error(json.toString)
    }

    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }

  implicit val googlePlayAppFormat = jsonFormat7(GooglePlayApp)

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat2(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)

  implicit val apiResolvedPackageInfoFormat = jsonFormat7(ApiResolvedPackageInfo)

  implicit val apiGetCollectionByPublicIdentifierResponseFormat = jsonFormat13(ApiGetCollectionByPublicIdentifierResponse)
}
