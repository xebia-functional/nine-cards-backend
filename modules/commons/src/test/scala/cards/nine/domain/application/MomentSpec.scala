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
package cards.nine.domain.application

import cards.nine.domain.ScalaCheck._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class MomentSpec
  extends Specification
  with ScalaCheck {

  "isMoment" should {
    "return true if a moment name is passed" in {
      prop { moment: Moment ⇒
        Moment.isMoment(moment.entryName) must beTrue
      }
    }

    "return true if a widget moment name is passed" in {
      prop { moment: Moment ⇒
        Moment.isMoment(s"${Moment.widgetMomentPrefix}${moment.entryName}") must beTrue
      }
    }

    "return true if a widget moment name is passed" in {
      prop { category: Category ⇒
        Moment.isMoment(category.entryName) must beFalse
      }
    }
  }
}
