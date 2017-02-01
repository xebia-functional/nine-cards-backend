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
package cards.nine.api.applications

import cards.nine.domain.application.{ BasicCard, CardList, FullCard }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  import Converters._

  "toApiCategorizeAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiCategorizeAppsResponse object" in {
      prop { (response: CardList[FullCard]) ⇒

        val apiResponse = toApiAppsInfoResponse(toApiCategorizedApp)(response)

        apiResponse.errors must containTheSameElementsAs(response.missing)

        forall(apiResponse.items) { item ⇒
          response.cards.exists(appInfo ⇒
            appInfo.packageName == item.packageName &&
              appInfo.categories == item.categories)
        }
      }
    }
  }

  "toApiDetailAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiDetailAppsResponse object" in {
      prop { (response: CardList[FullCard]) ⇒

        val apiResponse = toApiAppsInfoResponse(toApiDetailsApp)(response)

        apiResponse.errors must_== response.missing
        apiResponse.items must_== (response.cards map toApiDetailsApp)
      }
    }
  }

  "toApiIconApp" should {
    "convert a FullCard to an ApiAppIcon" in
      prop { (card: BasicCard) ⇒
        val api = toApiIconApp(card)
        api.packageName must_== card.packageName
        api.title must_== card.title
        api.icon must_== card.icon
      }
  }

}
