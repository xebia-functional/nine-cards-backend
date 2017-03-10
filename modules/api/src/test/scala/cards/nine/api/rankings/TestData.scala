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

import cards.nine.api.NineCardsHeaders._
import cards.nine.api.rankings.{ messages ⇒ Api }
import cards.nine.domain.analytics.RankedWidgetsByMoment
import cards.nine.domain.application.{ Category, Package }
import cards.nine.processes.rankings.messages.{ Get, Reload }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import org.joda.time.{ DateTime, DateTimeZone }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.headers.RawHeader
import cats.data.NonEmptyList

private[rankings] object TestData {

  val googleAnalyticsToken = "yada-yada-yada"

  val location = Option("US")

  val now = DateTime.now

  object Headers {

    val googleAnalyticsHeaders = NonEmptyList.of(
      RawHeader(headerGoogleAnalyticsToken, googleAnalyticsToken)
    )
  }

  object Messages {

    val getRankedWidgetsResponse = List.empty[RankedWidgetsByMoment]

    val ranking = GoogleAnalyticsRanking(Map(
      Category.SOCIAL.entryName → List(Package("testApp"))
    ))
    val getResponse = Get.Response(ranking)

    val apiRanking = Api.Ranking(
      Map(
        Category.SOCIAL.entryName → List("socialite", "socialist").map(Package),
        Category.COMMUNICATION.entryName → List("es.elpais", "es.elmundo", "uk.theguardian").map(Package)
      )
    )

    val reloadResponse = Reload.Response()
    val startDate: DateTime = new DateTime(2016, 7, 15, 0, 0, DateTimeZone.UTC)
    val endDate: DateTime = new DateTime(2016, 8, 21, 0, 0, DateTimeZone.UTC)
    val reloadApiRequest = Api.Reload.Request(startDate, endDate, 5)
    val reloadApiResponse = Api.Reload.Response()

  }

}
