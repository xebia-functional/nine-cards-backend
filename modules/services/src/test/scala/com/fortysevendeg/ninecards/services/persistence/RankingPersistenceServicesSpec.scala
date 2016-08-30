package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{ Category, PackageName }
import com.fortysevendeg.ninecards.services.free.domain.rankings._
import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities._
import doobie.imports.ConnectionIO
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import scalaz.std.list.listInstance

class RankingPersistenceServicesSpec
  extends Specification
  with BeforeEach
  with ScalaCheck
  with DomainDatabaseContext
  with NineCardsScalacheckGen
  with DisjunctionMatchers {

  import CustomComposite._

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

  "getRanking" should {

    "return an empty ranking if the table is empty" in prop {
      (scope: GeoScope) ⇒
        val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun
        storeRanking shouldEqual Ranking(Map.empty)
    }

    "read the ranking for the country" in prop { (scope: GeoScope, entries: List[Entry]) ⇒
      val id: Long = (for {
        d ← deleteItems(Queries.deleteBy(scope))
        i ← insertItems[List, Entry](Queries.insertBy(scope), entries)
      } yield i).transactAndRun

      val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun

      val cats = storeRanking.categories

      lazy val checkKeys = {
        val actualKeys = cats.keySet.toList.sortBy(_.entryName)
        val expected: List[Category] = entries.map(_.category).sortBy(_.entryName).distinct
        val mess = s"Actual keys ${cats.keySet} of ranking $storeRanking does not match expected $expected"
        (actualKeys == expected) :| mess
      }
      def checkEntry(entry: Entry) =
        cats.get(entry.category) must beSome[CategoryRanking].which { catRank ⇒
          catRank.ranking.contains(entry.packageName) // indexOf(entry.packageName) mustEqual (entry.ranking - 1)
        }

      lazy val checkEntries = {
        val mess = s"Ranking $storeRanking does not correspond to the entries"
        (entries forall checkEntry) :| mess
      }

      checkKeys && checkEntries
    }

  }

  "setRanking" should {

    "set an empty ranking to an empty table" in {
      prop { (scope: GeoScope) ⇒
        val pp = rankingPersistenceServices
          .setRanking(scope, Ranking(Map.empty))
          .transactAndRun

        val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun

        storeRanking shouldEqual Ranking(Map.empty)
      }
    }

    "set a non-empty ranking to a table" in {
      prop { (scope: GeoScope, ranking: Ranking) ⇒
        val pp = rankingPersistenceServices
          .setRanking(scope, ranking)
          .transactAndRun

        val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun
        val expected = Ranking(ranking.categories.filterNot(_._2.ranking.isEmpty))
        storeRanking shouldEqual expected
      }
    }
  }

}
