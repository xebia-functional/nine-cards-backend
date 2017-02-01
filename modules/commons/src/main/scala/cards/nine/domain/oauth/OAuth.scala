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
package cards.nine.domain.oauth

/**
  * In the Google OAuth Service, a Service Account is used to grant a server application
  * access to data that belongs not to a user, but to the application itself.
  * http://developers.google.com/identity/protocols/OAuth2ServiceAccount
  */
case class ServiceAccount(
  clientId: String,
  clientEmail: String,
  privateKey: String,
  privateKeyId: String,
  tokenUri: String,
  scopes: List[String]
)

/**
  * An AccessToken object contains a String whose value is used
  *  as an OAuth 2.0 Access Token, as defined in
  *  https://tools.ietf.org/html/rfc6749#section-1.4
  */
case class AccessToken(value: String) extends AnyVal
