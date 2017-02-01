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
package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.FullCard
import cards.nine.domain.market.{ MarketCredentials, MarketToken }
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.service.free.interpreter.TestData._
import cards.nine.googleplay.util.WithHttp1Client
import cats.syntax.either._
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import scalaz.concurrent.Task

class InterpretersIntegration extends Specification with WithHttp1Client {

  import TaskMatchers._

  private val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

  sequential

  "Http4sGooglePlayWebScraper, the parser of Google Play's pages" should {

    val auth = MarketCredentials(AndroidId(""), MarketToken(""), Some(localization))

    "result in an FullCard for packages that exist" in {
      val appRequest = AppRequest(fisherPrice.packageObj, auth)
      val response: Task[InfoError Either FullCard] = webClient.getCard(appRequest)
      val relevantDetails = response map { fullCard ⇒
        fullCard map { c: FullCard ⇒
          (c.packageName, c.categories, c.title)
        }
      }
      val expected = (fisherPrice.packageObj, fisherPrice.card.categories, fisherPrice.card.title)
      relevantDetails must returnValue(Right(expected))
    }

    "result in an error state for packages that do not exist" in {
      val appRequest = AppRequest(nonexisting.packageObj, auth)
      val response = webClient.getCard(appRequest)
      response must returnValue(Left(nonexisting.infoError))
    }

  }

}
