package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import com.fortysevendeg.ninecards.googleplay.domain.{GoogleAuthParams, Localization}
import org.http4s.{Header, Headers}

/* Note of development: this set of headers were directly copied from the code of the
 * google-play-crawler, but it is not clear what functions they perform. */
object headers {

  def fullHeaders(auth: GoogleAuthParams) = Headers( authHeaders(auth) ++ fixedHeaders)

  private[this] def authHeaders(auth: GoogleAuthParams): List[Header] = {
    Header("Authorization", s"GoogleLogin auth=${auth.token.value}") ::
    Header("X-DFE-Device-Id", auth.androidId.value) :: (
      auth.localization match {
        case Some(Localization(locale)) => List( Header("Accept-Language", locale) )
        case None => List()
      }
    )
  }

  private[this] lazy val fixedHeaders: List[Header] = {
    val userAgentValue = {
      val ls = List(
        "api=3", "versionCode=8016014", "sdk=15",
        "device=GT-I9300", "hardware=aries", "product=GT-I9300"
      ).mkString(",")
      s"Android-Finsky/3.10.14 (${ls})"
    }
    val unsopportedExperimentsValue = List(
      "nocache:billing.use_charging_poller",
      "market_emails",
      "buyer_currency",
      "prod_baseline",
      "checkin.set_asset_paid_app_field",
      "shekel_test",
      "content_ratings",
      "buyer_currency_in_app",
      "nocache:encrypted_apk",
      "recent_changes"
    ).mkString(",")

    List(
      Header("Accept-Language", "en-EN"),
      Header("Content-Type", "application/json; charset=UTF-8"),
      Header("Host", "android.clients.google.com"),
      Header("User-Agent", userAgentValue),
      Header("X-DFE-Unsupported-Experiments", unsopportedExperimentsValue),
      Header("X-DFE-Client-Id", "am-android-google"),
      Header("X-DFE-Enabled-Experiments", "cl:billing.select_add_instrument_by_default"),
      Header("X-DFE-Filter-Level", "3"),
      Header("X-DFE-SmallestScreenWidthDp", "320")
    )
  }

}