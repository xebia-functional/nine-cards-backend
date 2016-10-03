package cards.nine.services.free.interpreter.ranking

import cards.nine.services.free.domain.Category
import cards.nine.services.free.domain.rankings.{ CategoryRanking, Entry, GeoScope, Queries, RankedApp, Ranking, UnrankedApp }
import cards.nine.services.persistence.{ CustomComposite, DomainDatabaseContext, NineCardsScalacheckGen }
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

import scalaz.std.list._

class ServicesSpec
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

  "updateRanking" should {

    "set an empty ranking to an empty table" in {
      prop { (scope: GeoScope) ⇒
        val pp = rankingPersistenceServices
          .updateRanking(scope, Ranking(Map.empty))
          .transactAndRun

        val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun

        storeRanking shouldEqual Ranking(Map.empty)
      }
    }

    "set a non-empty ranking to a table" in {
      prop { (scope: GeoScope, ranking: Ranking) ⇒
        val pp = rankingPersistenceServices
          .updateRanking(scope, ranking)
          .transactAndRun

        val storeRanking = rankingPersistenceServices.getRanking(scope).transactAndRun
        val expected = Ranking(ranking.categories.filterNot(_._2.ranking.isEmpty))
        storeRanking shouldEqual expected
      }
    }
  }

  "getRankingForApps" should {

    "return an empty list of ranked apps if the table is empty" in {
      prop { (scope: GeoScope, deviceApps: Set[UnrankedApp]) ⇒
        val rankedApps = rankingPersistenceServices
          .getRankingForApps(scope, deviceApps)
          .transactAndRun

        rankedApps must beRight[List[RankedApp]](Nil)
      }
    }

    "return an empty list of ranked apps if an empty list of device apps is given" in {
      prop { scope: GeoScope ⇒
        val rankedApps = rankingPersistenceServices
          .getRankingForApps(scope, Set.empty)
          .transactAndRun

        rankedApps must beRight[List[RankedApp]](Nil)
      }
    }

    "return a list of ranked apps for the given list of device apps" in {
      prop { (scope: GeoScope, ranking: Ranking) ⇒
        rankingPersistenceServices
          .updateRanking(scope, ranking)
          .transactAndRun

        val deviceApps = ranking.categories.values
          .flatMap(_.ranking)
          .collect { case p if p.name.length > 10 ⇒ UnrankedApp(p.name) }
          .toSet

        val rankedApps = rankingPersistenceServices
          .getRankingForApps(scope, deviceApps)
          .transactAndRun

        rankedApps must beRight[List[RankedApp]].which { c ⇒
          c must haveSize(be_>=(deviceApps.size))
        }
      }
    }
  }
}
