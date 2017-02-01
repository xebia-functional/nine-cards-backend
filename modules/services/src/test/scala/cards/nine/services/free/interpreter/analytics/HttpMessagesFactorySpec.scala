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
package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics.DateRange
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.interpreter.analytics.HttpMessagesFactory.CountriesWithRankingReport
import cards.nine.services.free.interpreter.analytics.model.RequestBody
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class HttpMessagesFactorySpec extends Specification with ScalaCheck {

  "CountriesWithRankingReport.buildRequest" should {
    "create a valid RequestBody for the CountriesWithRankingReport" in {
      prop { (dateRange: DateRange, viewId: String) ⇒
        CountriesWithRankingReport.buildRequest(dateRange, viewId) must beLike[RequestBody] {
          case requestBody: RequestBody ⇒
            requestBody.reportRequests must haveSize(1)
        }
      }
    }
  }
}
