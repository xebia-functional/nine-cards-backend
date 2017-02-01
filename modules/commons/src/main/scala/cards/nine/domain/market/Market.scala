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
package cards.nine.domain.market

import cards.nine.domain.account.AndroidId

case class MarketToken(value: String) extends AnyVal

/**
  * A Market Localization contains a string that identifies the language
  * and the country for which the Market's information should be given.
  * Some examples are "es_ES", or "en_GB".
  */
case class Localization(value: String) extends AnyVal

case class MarketCredentials(
  androidId: AndroidId,
  token: MarketToken,
  localization: Option[Localization]
)
