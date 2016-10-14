package cards.nine.services.free.interpreter.ranking

import java.security.MessageDigest
import java.util.UUID

import cards.nine.commons.NineCardsService.Result
import cards.nine.services.free.algebra.Ranking._
import cards.nine.services.free.domain.{ Category, Moments }
import cards.nine.services.free.domain.rankings._
import cards.nine.services.persistence.{ CustomComposite, Persistence }
import cats.syntax.either._
import cats.~>
import doobie.imports._

import scalaz.std.list._
import scalaz.std.set._

class Services(persistence: Persistence[Entry]) extends (Ops ~> ConnectionIO) {

  import CustomComposite._

  val digest = MessageDigest.getInstance("MD5")

  def getRanking(scope: GeoScope): ConnectionIO[Ranking] = {
    def fromEntries(entries: List[Entry]): Ranking = {
      def toCategoryRanking(catEntries: List[Entry]): CategoryRanking =
        CategoryRanking(catEntries.sortBy(_.ranking).map(_.packageName))

      Ranking(entries.groupBy(_.category).mapValues(toCategoryRanking))
    }

    persistence.fetchList(Queries.getBy(scope)).map(fromEntries)
  }

  def updateRanking(scope: GeoScope, ranking: Ranking): ConnectionIO[UpdateRankingSummary] = {
    def toEntries(ranking: Ranking): List[Entry] = {
      def toEntries(category: Category, catrank: CategoryRanking): List[Entry] =
        catrank.ranking.zipWithIndex.map {
          case (packageName, index0) ⇒ Entry(packageName, category, index0 + 1)
        }

      ranking.categories.toList.flatMap((toEntries _).tupled)
    }

    for {
      del ← persistence update Queries.deleteBy(scope)
      ins ← persistence updateMany (Queries.insertBy(scope), toEntries(ranking))
    } yield UpdateRankingSummary(ins, del)
  }

  def getRankingForApps(
    scope: GeoScope,
    unrankedApps: Set[UnrankedApp]
  ): ConnectionIO[Result[List[RankedApp]]] = {
    val deviceAppTableName = generateTableName
    val moments = Moments.all map (_.entryName)
    val query = Queries.getRankedApps(scope, deviceAppTableName, moments)

    for {
      _ ← persistence.update(Queries.createDeviceAppsTemporaryTableSql(deviceAppTableName))
      _ ← persistence.updateMany(Queries.insertDeviceApps(deviceAppTableName), unrankedApps)
      rankedApps ← persistence.fetchListAs[RankedApp](query)
      _ ← persistence.update(Queries.dropDeviceAppsTemporaryTableSql(deviceAppTableName))
    } yield Either.right(rankedApps)
  }

  private[this] def generateTableName = {
    val text = UUID.randomUUID().toString
    val hash = digest.digest(text.getBytes).map("%02x".format(_)).mkString
    s"device_apps_$hash"
  }

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case GetRankingForApps(scope, apps) ⇒
      getRankingForApps(scope, apps)
    case GetRanking(scope) ⇒
      getRanking(scope)
    case UpdateRanking(scope, ranking) ⇒
      updateRanking(scope, ranking)
  }
}

object Services {

  def services(implicit persistence: Persistence[Entry]) = new Services(persistence)
}
