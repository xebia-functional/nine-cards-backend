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
package cards.nine.api.accounts

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.accounts.messages.{ ApiLoginRequest, ApiUpdateInstallationRequest }
import cards.nine.domain.account._
import cards.nine.processes.account.messages._
import org.joda.time.DateTime
import akka.http.scaladsl.model.headers.RawHeader

private[accounts] object TestData {

  val androidId = AndroidId("aaaaaaaaaaaaaaaa")

  val apiToken = ApiKey("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

  val authToken = "aaaaaaaa-bbbb-bbbb-bbbb-cccccccccccc"

  val deviceToken = Option(DeviceToken("77777777-7777-7777-7777-777777777777"))

  val email = Email("valid.email@test.com")

  val failingAuthToken = "abcdef012"

  val location = Option("US")

  val now = DateTime.now

  val sessionToken = SessionToken("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

  val tokenId = GoogleIdToken("66666666-6666-6666-6666-666666666666")

  val userId = 1l

  object Headers {

    val userInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, authToken)
    )

    val failingUserInfoHeaders = List(
      RawHeader(headerAndroidId, androidId.value),
      RawHeader(headerSessionToken, sessionToken.value),
      RawHeader(headerAuthToken, failingAuthToken)
    )

  }

  object Messages {

    val apiLoginRequest = ApiLoginRequest(email, androidId, tokenId)

    val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

    val loginRequest = LoginRequest(email, androidId, sessionToken, tokenId)

    val loginResponse = LoginResponse(apiToken, sessionToken)

    val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

    val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

  }

  object Paths {

    val installations = "/installations"

    val login = "/login"

  }

}
