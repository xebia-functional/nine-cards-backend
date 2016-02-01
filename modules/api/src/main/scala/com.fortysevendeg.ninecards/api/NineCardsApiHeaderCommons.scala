package com.fortysevendeg.ninecards.api

import shapeless.HList._
import shapeless.HNil
import spray.http.StatusCodes
import spray.routing.directives.HeaderDirectives._
import spray.routing.{HttpService, MissingHeaderRejection, RejectionHandler}

object NineCardsApiHeaderCommons {

  val headerAppslyAppId = "X-Appsly-Application-Id"
  val headerAppslyAPIKey = "X-Appsly-REST-API-Key"
  val headerAppslySessionToken = "X-Appsly-Session-Token"
  val headerAndroidId = "X-Android-ID"
  val headerMarketLocalization = "X-Android-Market-Localization"

  def requestLoginHeaders = for {
    appId <- headerValueByName(headerAppslyAppId)
    apiKey <- headerValueByName(headerAppslyAPIKey)
  } yield appId :: apiKey :: HNil

  def requestFullHeaders = requestLoginHeaders & (for {
    sessionToken <- headerValueByName(headerAppslySessionToken)
    androidId <- headerValueByName(headerAndroidId)
    marketLocalization <- headerValueByName(headerMarketLocalization)
  } yield sessionToken :: androidId :: marketLocalization :: HNil)
}

trait AuthHeadersRejectionHandler extends HttpService {
  implicit val authHeadersRejectionHandler = RejectionHandler {
    case MissingHeaderRejection(headerName: String) :: _ =>
      complete(StatusCodes.Unauthorized, "Missing authorization headers needed for this request")
  }
}