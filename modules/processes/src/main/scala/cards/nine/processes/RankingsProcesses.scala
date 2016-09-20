package cards.nine.processes

import cats.data.Xor
import cats.std.list._
import cats.std.map._
import cats.syntax.semigroup._
import cats.free.Free
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps.{ DeviceApp, RankedDeviceApp }
import cards.nine.processes.messages.rankings._
import cards.nine.services.common.ConnectionIOOps._
import cards.nine.services.free.algebra.DBResult.DBOps
import cards.nine.services.free.algebra.GoogleAnalytics
import cards.nine.services.free.domain.rankings._
import cards.nine.services.persistence._
import cards.nine.services.persistence.rankings.{ Services ⇒ PersistenceServices }
import doobie.imports._

import scalaz.concurrent.Task

class RankingProcesses[F[_]](
  implicit
  analytics: GoogleAnalytics.Services[F],
  persistence: PersistenceServices,
  transactor: Transactor[Task],
  dbOps: DBOps[F]
) {

  def getRanking(scope: GeoScope): Free[F, Get.Response] =
    persistence.getRanking(scope).map(Get.Response.apply).liftF[F]

  def reloadRanking(scope: GeoScope, params: RankingParams): Free[F, Reload.XorResponse] =
    for /*Free[F]*/ {
      rankingTry ← analytics.getRanking(scope, params)
      res ← rankingTry match {
        case Xor.Left(error) ⇒ errorAux(error)
        case Xor.Right(ranking) ⇒ setAux(scope, ranking)
      }
    } yield res

  def getRankedDeviceApps(
    scope: GeoScope,
    deviceApps: Map[String, List[DeviceApp]]
  ): Free[F, Map[String, List[RankedDeviceApp]]] = {

    def findAppsWithoutRanking(apps: List[DeviceApp], rankings: List[RankedDeviceApp]) =
      apps.collect {
        case app if !rankings.exists(_.packageName == app.packageName) ⇒
          RankedDeviceApp(app.packageName, None)

      }

    if (deviceApps.isEmpty)
      Free.pure(Map.empty)
    else
      (persistence.getRankedApps(scope, deviceApps.values.flatten.toSet map toUnrankedApp) map {
        rankedApps ⇒
          val rankedAppsByCategory = rankedApps.groupBy(_.category).mapValues(_.map(toRankedDeviceApp))

          val unrankedDeviceApps = deviceApps map {
            case (category, apps) ⇒
              (category, findAppsWithoutRanking(apps, rankedAppsByCategory.getOrElse(category, Nil)))
          }

          rankedAppsByCategory.combine(unrankedDeviceApps)
      }).liftF
  }

  private[this] def setAux(scope: GeoScope, ranking: Ranking): Free[F, Reload.XorResponse] =
    persistence.setRanking(scope, ranking).liftF[F].map(_ ⇒ Xor.Right(Reload.Response()))

  private[this] def errorAux(error: RankingError): Free[F, Reload.XorResponse] = {
    val procError = Reload.Error(error.code, error.message, error.status)
    Free.pure[F, Reload.XorResponse](Xor.Left(procError))
  }

}

object RankingProcesses {

  implicit def processes[F[_]](
    implicit
    analytics: GoogleAnalytics.Services[F],
    persistence: PersistenceServices,
    dbOps: DBOps[F]
  ) = new RankingProcesses

}