package cards.nine.processes

import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps.{ DeviceApp, RankedDeviceApp }
import cards.nine.processes.messages.rankings._
import cards.nine.services.common.FreeUtils._
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
    deviceApps: Map[String, List[DeviceApp]]
  ): Free[F, Map[String, List[RankedDeviceApp]]] = {

    def findAppsWithoutRanking(apps: List[DeviceApp], rankings: List[RankedDeviceApp]) =
      apps.collect {
        case app if !rankings.exists(_.packageName == app.packageName) ⇒
          RankedDeviceApp(app.packageName, None)

      }

    def geoScopeFromLocation(isoCode: String): Free[F, GeoScope] =
      countryPersistence.getCountryByIsoCode2(isoCode.toUpperCase) map {
        case Some(country) ⇒ country.toGeoScope
        case _ ⇒ WorldScope
      }

    if (deviceApps.isEmpty)
      Map.empty[String, List[RankedDeviceApp]].toFree
    else {
      for {
        geoScope ← location.fold(Free.pure[F, GeoScope](WorldScope))(geoScopeFromLocation)
        rankedApps ← rankingPersistence.getRankingForApps(geoScope, deviceApps.values.flatten.toSet map toUnrankedApp)
        rankedAppsByCategory = rankedApps.groupBy(_.category).mapValues(_.map(toRankedDeviceApp))
        unrankedDeviceApps = deviceApps map {
          case (category, apps) ⇒
            (category, findAppsWithoutRanking(apps, rankedAppsByCategory.getOrElse(category, Nil)))
        }
      } yield rankedAppsByCategory.combine(unrankedDeviceApps)
    }
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