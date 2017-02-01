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
package cards.nine.app

import akka.actor.Actor
import akka.event.LoggingAdapter
import cards.nine.app.RankingActor.RankingByCategory
import cards.nine.commons.NineCardsErrors._
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.commons.config.Domain.RankingsOAuthConfiguration
import cards.nine.domain.analytics._
import cards.nine.domain.oauth.ServiceAccount
import cards.nine.domain.pagination.Page
import cards.nine.processes.rankings.RankingProcesses
import cards.nine.processes.rankings.messages.Reload._
import cats.~>
import cats.free.Free
import org.joda.time.{ DateTime, DateTimeZone }
import scalaz.{ -\/, \/, \/- }
import scalaz.concurrent.Task
import shapeless.LabelledGeneric

class RankingActor[F[_]](interpreter: F ~> Task, log: LoggingAdapter)(implicit rankingProcesses: RankingProcesses[F]) extends Actor {

  private[this] def generateRankings: Free[F, Result[SummaryResponse]] = {
    import nineCardsConfiguration.rankings._

    val now = DateTime.now(DateTimeZone.UTC)
    val today = now.withTimeAtStartOfDay

    val pageNumber = ((now.getDayOfWeek - 1) * 24 + now.getHourOfDay) * countriesPerRequest

    val serviceAccount = {
      val oauthConfigLG = LabelledGeneric[RankingsOAuthConfiguration]
      val serviceAccountLG = LabelledGeneric[ServiceAccount]
      serviceAccountLG.from(oauthConfigLG.to(oauth))
    }

    rankingProcesses.reloadRankingForCountries(
      Request(
        DateRange(
          today.minus(rankingPeriod),
          today
        ),
        maxNumberOfAppsPerCategory,
        serviceAccount,
        Page(pageNumber, countriesPerRequest)
      )
    )
  }

  private[this] def showRankingGenerationInfo(result: Throwable \/ Result[SummaryResponse]) = {
    result match {
      case -\/(e) ⇒ log.error(e, "An error was found while generating rankings")
      case \/-(Left(e)) ⇒ reportNineCardsError(e)
      case \/-(Right(summary)) ⇒
        showCountriesWithoutRankingInfo(summary.countriesWithoutRanking)
        summary.countriesWithRanking foreach showRankingSummary
        log.info("Rankings generated successfully")
    }
  }

  private[this] def showCountriesWithoutRankingInfo(countriesCode: List[CountryIsoCode]) =
    if (countriesCode.nonEmpty)
      log.info(s"Skipped countries without ranking info: ${countriesCode.map(_.value).mkString(",")}")

  private[this] def showRankingSummary(summary: UpdateRankingSummary) =
    summary.countryCode match {
      case None ⇒ log.info(s"World ranking generated: ${summary.created} entries created")
      case Some(code) ⇒ log.info(s"Ranking generated for ${code.value}: ${summary.created} entries created")
    }

  private[this] def reportNineCardsError(error: NineCardsError): Unit = {
    log.error("An error was found while generating rankings")
    error match {
      case GoogleOAuthError(message) ⇒
        log.error("OAuth failure: there was a failure when retrieving an access token")
        log.error(message)
      case GoogleAnalyticsServerError(message) ⇒
        log.error("Google Analytics failure: there was a problem in retrieving the Google Analytics Report")
        log.error(message)
      case _ ⇒ Unit
    }
  }

  def receive = {
    case RankingByCategory ⇒
      log.info("Running actor for generate rankings ...")
      generateRankings.foldMap(interpreter).unsafePerformAsync(showRankingGenerationInfo)
  }
}

object RankingActor {
  val RankingByCategory = "ranking"
}
