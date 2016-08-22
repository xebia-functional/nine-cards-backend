package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import com.fortysevendeg.ninecards.services.free.domain.{ Category, PackageName }
import com.fortysevendeg.ninecards.services.free.domain.rankings._
import scalaz.std.iterable._

object ranking {

  class Services(persistence: Persistence[Entry]) {

    import CustomComposite._

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

  }

  object Services {

    implicit def services(implicit persistence: Persistence[Entry]): Services =
      new Services(persistence)
  }
}

