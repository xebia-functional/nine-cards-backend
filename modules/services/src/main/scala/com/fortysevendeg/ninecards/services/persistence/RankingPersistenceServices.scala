package com.fortysevendeg.ninecards.services.persistence

import java.security.MessageDigest
import java.util.UUID

import doobie.imports._
import com.fortysevendeg.ninecards.services.free.domain.Category
import com.fortysevendeg.ninecards.services.free.domain.rankings._

import scalaz.std.iterable._

object rankings {

  class Services(persistence: Persistence[Entry]) {

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

    def setRanking(scope: GeoScope, ranking: Ranking): ConnectionIO[(Int, Int)] = {
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
      } yield (ins, del)
    }

    def getRankedApps(scope: GeoScope, unrankedApps: Set[UnrankedApp]): ConnectionIO[List[RankedApp]] = {
      val deviceAppTableName = generateTableName

      for {
        _ ← persistence.update(Queries.createDeviceAppsTemporaryTableSql(deviceAppTableName))
        _ ← persistence.updateMany(Queries.insertDeviceApps(deviceAppTableName), unrankedApps)
        rankedApps ← persistence.fetchListAs[RankedApp](Queries.getRankedApps(scope, deviceAppTableName))
        _ ← persistence.update(Queries.dropDeviceAppsTemporaryTableSql(deviceAppTableName))
      } yield rankedApps
    }

    private[this] def generateTableName = {
      val text = UUID.randomUUID().toString
      val hash = digest.digest(text.getBytes).map("%02x".format(_)).mkString
      s"device_apps_$hash"
    }
  }

  object Services {

    implicit def services(implicit persistence: Persistence[Entry]): Services =
      new Services(persistence)
  }
}

