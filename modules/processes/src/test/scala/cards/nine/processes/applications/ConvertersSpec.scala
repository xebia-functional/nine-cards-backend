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
package cards.nine.processes.applications

import cards.nine.domain.application.{ CardList, FullCard }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  import Converters._

  "filterCategorized" should {
    "convert an AppsInfo to a GetAppsInfoResponse object" in {
      prop { appsInfo: CardList[FullCard] ⇒

        val appsWithoutCategories = appsInfo.cards.filter(app ⇒ app.categories.isEmpty)

        val categorizeAppsResponse = filterCategorized(appsInfo)

        categorizeAppsResponse.missing shouldEqual appsInfo.missing ++ appsWithoutCategories.map(_.packageName)

        forall(categorizeAppsResponse.cards) { item ⇒
          appsInfo.cards.exists { app ⇒
            app.packageName == item.packageName &&
              app.categories == item.categories &&
              app.categories.nonEmpty
          } should beTrue
        }
      }
    }
  }

}
