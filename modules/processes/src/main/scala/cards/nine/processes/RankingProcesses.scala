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
import cats.free.Free
import cats.instances.list._
import cats.instances.map._
import cats.syntax.semigroup._

class RankingProcesses[F[_]](
  implicit
  analytics: GoogleAnalytics.Services[F],
  countryPersistence: algebra.Country.Services[F],
  rankingServices: algebra.Ranking.Services[F]
) {

  def getRanking(scope: GeoScope): Free[F, Get.Response] =
    rankingServices.getRanking(scope) map Get.Response

  def reloadRanking(scope: GeoScope, params: RankingParams): Free[F, Result[Reload.Response]] = {

    def getCountryName(scope: GeoScope): NineCardsService[F, Option[CountryName]] = scope match {
      case WorldScope ⇒ NineCardsService.right[F, Option[CountryName]](None)
      case CountryScope(code) ⇒
        countryPersistence.getCountryByIsoCode2(code.value.toUpperCase).map(c ⇒ Option(CountryName(c.name)))
    }

    for {
      countryName ← getCountryName(scope)
      ranking ← analytics.getRanking(countryName, params)
      _ ← rankingServices.updateRanking(scope, ranking)
    } yield Reload.Response()
  }.value

  def getRankedDeviceApps(
    location: Option[String],
    deviceApps: Map[String, List[Package]]
  ): Free[F, Result[Map[String, List[RankedApp]]]] = {

    def geoScopeFromLocation(isoCode: String): NineCardsService[F, GeoScope] =
      countryPersistence.getCountryByIsoCode2(isoCode.toUpperCase)
        .map { country ⇒
          val scope: GeoScope = CountryScope(CountryIsoCode(country.isoCode2))
          scope
        }
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
        rankedApps ← rankingServices.getRankingForApps(geoScope, unrankedApps)
        rankedAppsByCategory = rankedApps.groupBy(_.category)
        unrankedDeviceApps = unifiedDeviceApps map {
          case (category, apps) ⇒
            val appWithoutRanking = apps
              .diff(rankedAppsByCategory.getOrElse(category, Nil).map(_.packageName))
              .map(RankedApp(_, category, None))
            (category, appWithoutRanking)
        }
      } yield rankedAppsByCategory combine unrankedDeviceApps
    }.value
  }

}

object RankingProcesses {

  implicit def processes[F[_]](
    implicit
    analytics: GoogleAnalytics.Services[F],
    countryPersistence: algebra.Country.Services[F],
    rankingServices: algebra.Ranking.Services[F]
  ) = new RankingProcesses

}