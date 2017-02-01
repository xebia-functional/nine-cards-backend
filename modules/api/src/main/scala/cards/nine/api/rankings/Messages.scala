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
package cards.nine.api.rankings

import cards.nine.domain.application.Package

import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat

object messages {

  case class Ranking(categories: Map[String, List[Package]])

  object Reload {

    case class Request(
      startDate: DateTime,
      endDate: DateTime,
      rankingLength: Int
    ) {
      if (!isValidDateRange(startDate, endDate))
        throw new InvalidDate(startDate, endDate)
    }

    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC

    class InvalidDate(startDate: DateTime, endDate: DateTime) extends Throwable {
      override def getMessage(): String = {
        s"""Invalid date range { startDate=${formatter.print(startDate)}, endDate = ${formatter.print(endDate)} }
          | For a date range to be valid,
          | * The startDate and endDate must be formatted as "YYYY-MM-DD", such as "2016-04-15".
          | * The startDate must be before the endDate
          | * The startDate must be after Google Analytics launch date, in 2015-01-01"
        """.stripMargin
      }
    }

    private[this] val googleAnalyticsLaunch: DateTime =
      new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC)

    def isValidDateRange(startDate: DateTime, endDate: DateTime): Boolean = {
      (startDate isBefore endDate) && (startDate isAfter googleAnalyticsLaunch)
    }

    case class Response()

    case class Error(code: Int, message: String, status: String) extends Throwable

  }

  object Get {
    case class Response(ranking: Ranking)
  }

}
