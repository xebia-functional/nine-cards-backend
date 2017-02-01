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

import cards.nine.api.accounts.messages._
import cards.nine.domain.account._
import spray.json._

private[accounts] trait JsonFormats extends DefaultJsonProtocol {

  implicit object EmailJsonFormat extends JsonFormat[Email] {
    def read(json: JsValue): Email = Email(StringJsonFormat.read(json))
    def write(pack: Email): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object DeviceTokenJsonFormat extends JsonFormat[DeviceToken] {
    def read(json: JsValue): DeviceToken = DeviceToken(StringJsonFormat.read(json))
    def write(pack: DeviceToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object ApiKeyJsonFormat extends JsonFormat[ApiKey] {
    def read(json: JsValue): ApiKey = ApiKey(StringJsonFormat.read(json))
    def write(pack: ApiKey): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object AndroidIdJsonFormat extends JsonFormat[AndroidId] {
    def read(json: JsValue): AndroidId = AndroidId(StringJsonFormat.read(json))
    def write(pack: AndroidId): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object GoogleIdTokenJsonFormat extends JsonFormat[GoogleIdToken] {
    def read(json: JsValue): GoogleIdToken = GoogleIdToken(StringJsonFormat.read(json))
    def write(pack: GoogleIdToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit object SessionTokenJsonFormat extends JsonFormat[SessionToken] {
    def read(json: JsValue): SessionToken = SessionToken(StringJsonFormat.read(json))
    def write(pack: SessionToken): JsValue = StringJsonFormat.write(pack.value)
  }

  implicit val apiLoginRequestFormat = jsonFormat3(ApiLoginRequest)

  implicit val apiLoginResponseFormat = jsonFormat2(ApiLoginResponse)

  implicit val updateInstallationRequestFormat = jsonFormat1(ApiUpdateInstallationRequest)

  implicit val updateInstallationResponseFormat = jsonFormat2(ApiUpdateInstallationResponse)

}

object JsonFormats extends JsonFormats
