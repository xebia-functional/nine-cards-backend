package com.fortysevendeg.ninecards.api

import org.joda.time.DateTime

object NineCardsHeaders {

  val headerAndroidId = "X-Android-ID"
  val headerApiKey = "X-Appsly-REST-API-Key"
  val headerAppId = "X-Appsly-Application-Id"
  val headerMarketLocalization = "X-Android-Market-Localization"
  val headerSessionToken = "X-Appsly-Session-Token"
  val headerAuthToken = "X-Auth-Token"

  object Domain {

    case class AndroidId(value: String) extends AnyVal

    case class ApiKey(value: String) extends AnyVal

    case class ApplicationId(value: String) extends AnyVal

    case class AuthToken(value: String) extends AnyVal

    case class CurrentDateTime(value: DateTime) extends AnyVal

    case class MarketLocalization(value: String) extends AnyVal

    case class NewSharedCollectionInfo(currentDate: CurrentDateTime, identifier: PublicIdentifier)

    case class PublicIdentifier(value: String) extends AnyVal

    case class SessionToken(value: String) extends AnyVal

    case class UserId(value: Long) extends AnyVal

    case class UserContext(userId: UserId, androidId: AndroidId)

  }
}
