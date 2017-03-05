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
package cards.nine.api

import cards.nine.api.NineCardsHeaders._
import cards.nine.domain.account._
import org.joda.time.DateTime
import akka.http.scaladsl.model.headers.RawHeader

object TestData {

  val androidId = AndroidId("aaaaaaaaaaaaaaaa")

  val apiToken = ApiKey("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

  val author = "John Doe"

  val authToken = "dddddddd-dddd-dddd-dddd-dddddddddddd"

  val category = "SOCIAL"

  val deviceToken = Option(DeviceToken("77777777-7777-7777-7777-777777777777"))

  val email = Email("valid.email@test.com")

  val failingAuthToken = "abcdefg"

  val googlePlayToken = "111111111"

  val location = Option("US")

  val marketLocalization = "en-us"

  val now = DateTime.now

  val moments = List("HOME", "NIGHT")

  val sessionToken = SessionToken("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

  val userId = 1l

  object Headers {

    val userInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, authToken)
    )

    val googlePlayHeaders = List(
      RawHeader(headerGooglePlayToken, googlePlayToken),
      RawHeader(headerMarketLocalization, marketLocalization)
    )

    val failingUserInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, failingAuthToken)
    )

  }

}
