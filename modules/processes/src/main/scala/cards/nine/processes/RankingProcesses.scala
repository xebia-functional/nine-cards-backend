package cards.nine.processes

import cards.nine.commons.NineCardsErrors.CountryNotFound
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.analytics._
import cards.nine.domain.application.Package
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.rankings._
import cards.nine.services.free.algebra
import cards.nine.services.free.algebra.GoogleAnalytics
import cards.nine.services.free.domain.rankings._
import cats.data.Xor
import cats.free.Free
import cats.instances.list._
import cats.instances.map._
import cats.syntax.semigroup._

class RankingProcesses[F[_]](
  implicit
  analytics: GoogleAnalytics.Services[F],
  countryPersistence: algebra.Country.Services[F],
  rankingPersistence: algebra.Ranking.Services[F]
) {

  def getRanking(scope: GeoScope): Free[F, Get.Response] =
    rankingPersistence.getRanking(scope).map(Get.Response.apply)

  def reloadRanking(scope: GeoScope, params: RankingParams): Free[F, Reload.XorResponse] =
    for {
      rankingTry ← analytics.getRanking(scope, params)
      res ← rankingTry match {
        case Xor.Left(error) ⇒ errorAux(error)
        case Xor.Right(ranking) ⇒ setAux(scope, ranking)
      }
    } yield res

  def getRankedDeviceApps(
    location: Option[String],
    deviceApps: Map[String, List[Package]]
  ): Free[F, Result[Map[String, List[RankedApp]]]] = {

    def findAppsWithoutRanking(apps: List[Package], rankings: List[RankedApp], category: String) =
      apps.collect {
        case app if !rankings.exists(_.packageName == app) ⇒
          RankedApp(app, category, None)

      }

    def geoScopeFromLocation(isoCode: String): NineCardsService[F, GeoScope] =
      countryPersistence.getCountryByIsoCode2(isoCode.toUpperCase)
        .map(_.toGeoScope)
        .recover { case _: CountryNotFound ⇒ WorldScope }

    def unifyDeviceApps(deviceApps: Map[String, List[Package]]) = {
      val (games, otherApps) = deviceApps.partition { case (cat, apps) ⇒ cat.matches("GAME\\_.*") }

      if (games.isEmpty)
        otherApps
      else
        otherApps.combine(Map("GAME" → games.flatMap { case (cat, apps) ⇒ apps }.toList))
    }

    if (deviceApps.isEmpty)
      NineCardsService.right(Map.empty[String, List[RankedApp]]).value
    else {
      val unifiedDeviceApps = unifyDeviceApps(deviceApps)
      val unrankedApps = unifiedDeviceApps.flatMap {
        case (cat, apps) ⇒ apps map toUnrankedApp(cat)
      }.toSet

      for {
        geoScope ← location.fold(NineCardsService.right[F, GeoScope](WorldScope))(geoScopeFromLocation)
        rankedApps ← rankingPersistence.getRankingForApps(geoScope, unrankedApps)
        rankedAppsByCategory = rankedApps.groupBy(_.category)
        unrankedDeviceApps = unifiedDeviceApps map {
          case (category, apps) ⇒
            (category, findAppsWithoutRanking(apps, rankedAppsByCategory.getOrElse(category, Nil), category))
        }
      } yield rankedAppsByCategory combine unrankedDeviceApps
    }.value
  }

  private[this] def setAux(scope: GeoScope, ranking: Ranking): Free[F, Reload.XorResponse] =
    rankingPersistence.updateRanking(scope, ranking).map(_ ⇒ Xor.Right(Reload.Response()))

  private[this] def errorAux(error: RankingError): Free[F, Reload.XorResponse] = {
    val procError = Reload.Error(error.code, error.message, error.status)
    Free.pure[F, Reload.XorResponse](Xor.Left(procError))
  }

}

object RankingProcesses {

  implicit def processes[F[_]](
    implicit
    analytics: GoogleAnalytics.Services[F],
    countryPersistence: algebra.Country.Services[F],
    rankingPersistence: algebra.Ranking.Services[F]
  ) = new RankingProcesses

}