/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.domain.market.{ MarketCredentials, Localization }
import org.http4s.{ Header, Headers }

/* Note of development: this set of headers were directly copied from the code of the
 * google-play-crawler, but it is not clear what functions they perform. */
object headers {

  def fullHeaders(auth: MarketCredentials, contentType: Option[String] = None) =
    Headers(authHeaders(auth) ++ fixedHeaders)
      .put(Header("Content-Type", contentType.getOrElse("application/json; charset=UTF-8")))

  private[this] def authHeaders(auth: MarketCredentials): List[Header] = {
    Header("Authorization", s"GoogleLogin auth=${auth.token.value}") ::
      Header("X-DFE-Device-Id", auth.androidId.value) :: (
        auth.localization match {
          case Some(Localization(locale)) ⇒ List(Header("Accept-Language", locale))
          case None ⇒ List()
        }
      )
  }

  private[this] lazy val fixedHeaders: List[Header] = {
    val userAgentValue = {
      val ls = List(
        "api=3", "versionCode=8016014", "sdk=15",
        "device=GT-I9300", "hardware=aries", "product=GT-I9300"
      ).mkString(",")
      s"Android-Finsky/3.10.14 ($ls)"
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