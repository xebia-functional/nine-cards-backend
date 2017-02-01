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
package cards.nine.api.collections

import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol._
import spray.json._

class JsonFormatsSpec extends Specification with Matchers {

  sequential

  "JodaDateTimeFormat, the Json Format for dates," should {

    import cards.nine.api.collections.JsonFormats.JodaDateTimeFormat

    val date = new DateTime(2013, 5, 23, 0, 0, DateTimeZone.UTC)

    "transfer it to a Json string as format" in {
      date.toJson must beEqualTo(JsString("2013-05-23T00:00:00.000+0000"))
    }

    "parse it from a Json String" in {
      val str = """ "2013-05-23T00:00:00.000+0000" """
      str.parseJson.convertTo[DateTime] shouldEqual date
    }

    case class MyDate(date: DateTime)
    implicit val format = jsonFormat1(MyDate)

    "use it properly in a record" in {
      val str = """ { "date" : "2013-05-23T00:00:00.000Z" } """
      str.parseJson.convertTo[MyDate] shouldEqual MyDate(date)
    }

  }

}
