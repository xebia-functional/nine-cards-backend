package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.utils.DummyNineCardsConfig
import doobie.imports._
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2._
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

import scalaz.std.iterable._
import scalaz.\/

trait DatabaseContext extends DummyNineCardsConfig {

  private[this] val trx = transactor

  implicit class Transacting[A](operation: ConnectionIO[A]) {
    def transactAndRun(): A = operation.transact(trx).unsafePerformSync
    def transactAndAttempt(): \/[Throwable, A] = operation.transact(trx).unsafePerformSyncAttempt
  }

  case class PersistenceItem(id: Long, name: String, active: Boolean)

  val fetchAllSql = "SELECT id,name,active FROM persistence"
  val fetchAllActiveSql = "SELECT id,name,active FROM persistence WHERE active=true"
  val fetchByIdSql = "SELECT id,name,active FROM persistence WHERE id=?"
  val fetchByIdAndStatusSql = "SELECT id,name,active FROM persistence WHERE id=? AND active=?"
  val fetchByStatusSql = "SELECT id,name,active FROM persistence WHERE active=?"
  val insertSql = "INSERT INTO persistence (name,active) VALUES (?,?)"
  val updateAllSql = "UPDATE persistence SET active=false"
  val updateAllActiveSql = "UPDATE persistence SET active=false WHERE active=true"
  val updateByIdSql = "UPDATE persistence SET name=?,active=? WHERE id=?"
  val updateByStatusSql = "UPDATE persistence SET active=? WHERE active=?"

  def createTable: ConnectionIO[Int] =
    sql"""
          CREATE TABLE persistence (
          id   BIGINT AUTO_INCREMENT,
          name VARCHAR NOT NULL,
          active BOOL NOT NULL)""".update.run

  def dropTable: ConnectionIO[Int] = sql"""DROP TABLE IF EXISTS persistence""".update.run

  def insertItem(
    name: String,
    active: Boolean
  ): ConnectionIO[Long] =
    sql"INSERT INTO persistence (name, active) VALUES ($name,$active)".update.withUniqueGeneratedKeys[Long]("id")

  def insertItems(
    values: List[(String, Boolean)]
  ): ConnectionIO[Int] =
    Update[(String, Boolean)]("INSERT INTO persistence (name, active) VALUES (?,?)").updateMany(values)

  def fetchAll: ConnectionIO[List[(String, Boolean, Long)]] =
    sql"SELECT name,active,id FROM persistence".query[(String, Boolean, Long)].list

  def fetchItemById(
    id: Long
  ): ConnectionIO[PersistenceItem] =
    sql"SELECT id,name,active FROM persistence WHERE id=$id".query[PersistenceItem].unique

  def fetchItemByStatus(
    active: Boolean
  ): ConnectionIO[List[PersistenceItem]] =
    sql"SELECT id,name,active FROM persistence WHERE active=$active".query[PersistenceItem].list

  def fetchItemByStatuses(
    active: Boolean
  ): ConnectionIO[List[PersistenceItem]] = {
    val inactive = !active
    sql"SELECT id,name,active FROM persistence WHERE active=$active OR active=$inactive".query[PersistenceItem].list
  }

  val persistence = new Persistence[PersistenceItem]

  def genBoundedList[T](minSize: Int = 1, maxSize: Int = 100, gen: Gen[T]): Gen[List[T]] =
    Gen.choose(minSize, maxSize) flatMap { size ⇒ Gen.listOfN(size, gen) }

  implicit val dataWithId: Arbitrary[List[(Long, String, Boolean)]] =
    Arbitrary(
      genBoundedList(
        minSize = 2,
        maxSize = 10,
        gen     = Gen.resultOf((l: Long, s: String, b: Boolean) ⇒ (l, s, b))
      )
    )

  implicit val data: Arbitrary[List[(String, Boolean)]] =
    Arbitrary(genBoundedList(
      minSize = 2,
      maxSize = 10,
      gen     = Gen.resultOf((s: String, b: Boolean) ⇒ (s, b))
    ))

  implicit val stringList: Arbitrary[List[String]] =
    Arbitrary(genBoundedList(minSize = 2, gen = Gen.resultOf((s: String) ⇒ s)))
}

class PersistenceSpec
  extends Specification
  with BeforeEach
  with DatabaseContext
  with DisjunctionMatchers
  with ScalaCheck {

  sequential

  override def before = {
    for {
      _ ← dropTable
      _ ← createTable
    } yield ()
  }.transactAndRun

  "fetchList (SQL without parameters)" should {
    "return an empty list if the table is empty" in {
      prop { (i: Int) ⇒

        val list = persistence.fetchList(sql = fetchAllSql).transactAndRun

        list must beEmpty
      }
    }

    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
        prop { (data: List[(String, Boolean)]) ⇒
          insertItems(data).transactAndRun

          val list = persistence.fetchList(sql = fetchAllSql).transactAndRun

          list must not be empty
        }
      }

    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
        prop { (names: List[String]) ⇒
          val namesWithStatus = names map ((_, false))
          insertItems(namesWithStatus).transactAndRun

          val list = persistence.fetchList(sql = fetchAllActiveSql).transactAndRun

          list must beEmpty
        }
      }
  }

  "fetchList" should {
    "return an empty list if the table is empty" in {
      prop { (status: Boolean) ⇒
        val list = persistence.fetchList(
          sql    = fetchByStatusSql,
          values = status
        ).transactAndRun

        list must beEmpty
      }
    }
    "return a list of PersistenceItem if there are some elements in the table that meet the criteria" in {
      prop { (names: List[String]) ⇒
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transactAndRun

        val list = persistence.fetchList(
          sql    = fetchByStatusSql,
          values = true
        ).transactAndRun

        list must not be empty
        list.forall(item ⇒ item.active) must beTrue
      }
    }
    "return an empty list if there aren't any elements in the table that meet the criteria" in {
      prop { (names: List[String]) ⇒
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transactAndRun

        val list = persistence.fetchList(
          sql    = fetchByStatusSql,
          values = false
        ).transactAndRun

        list must beEmpty
      }
    }
  }

  "fetchOption" should {
    "return None if the table is empty" in {
      prop { (status: Boolean) ⇒
        val persistenceItem = persistence.fetchOption(
          sql    = fetchByStatusSql,
          values = status
        ).transactAndRun

        persistenceItem must beEmpty
      }
    }
    "return a PersistenceItem if there is an element in the table that meets the criteria" in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data
        val id = insertItem(name = name, active = active).transactAndRun

        val persistenceItem = persistence.fetchOption(
          sql    = fetchByIdAndStatusSql,
          values = (id, active)
        ).transactAndRun

        persistenceItem must beSome[PersistenceItem].which {
          item ⇒
            item.id mustEqual id
            item.name mustEqual name
            item.active mustEqual active
        }
      }
    }
    "return None if there isn't any element in the table that meets the criteria" in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data
        val id = insertItem(name = name, active = active).transactAndRun
        val persistenceItem = persistence.fetchOption(
          sql    = fetchByIdAndStatusSql,
          values = (id, !active)
        ).transactAndRun

        persistenceItem must beEmpty
      }
    }
    "throw an exception if there are more than one element in the table that meet the criteria" in {
      prop { (names: List[String]) ⇒
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transactAndRun

        persistence.fetchOption(
          sql    = fetchByStatusSql,
          values = true
        ).transactAndRun must throwA[Throwable]
      }
    }
  }

  "fetchUnique" should {
    "throw an exception if the table is empty" in {
      prop { (id: Long) ⇒
        persistence.fetchUnique(
          sql    = fetchByIdSql,
          values = id
        ).transactAndAttempt must be_-\/[Throwable]
      }
    }
    "return a PersistenceItem if there is an element in the table with the given id" in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data
        val id = insertItem(name = name, active = active).transactAndRun
        val item = persistence.fetchUnique(
          sql    = fetchByIdSql,
          values = id
        ).transactAndRun

        item.id mustEqual id
        item.name mustEqual name
      }
    }
    "throw an exception if there isn't any element in the table that meet the criteria" in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data
        val id = insertItem(name = name, active = active).transactAndRun

        persistence.fetchUnique(
          sql    = fetchByIdAndStatusSql,
          values = (id, !active)
        ).transactAndAttempt must be_-\/[Throwable]
      }
    }
    "throw an exception if there are more than one element in the table that meet the criteria" in {
      prop { (names: List[String]) ⇒
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transactAndRun

        persistence.fetchUnique(
          sql    = fetchByStatusSql,
          values = true
        ).transactAndRun must throwA[Throwable]
      }
    }
  }

  "update (SQL without parameters)" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
        prop { (i: Int) ⇒
          persistence.update(sql = updateAllSql).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual 0
          }
        }
      }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
        prop { (names: List[String]) ⇒
          val namesWithStatus = names map ((_, true))
          insertItems(namesWithStatus).transactAndRun

          persistence.update(updateAllActiveSql).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows must be greaterThan 0
          }
        }
      }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
        prop { (names: List[String]) ⇒
          val namesWithStatus = names map ((_, false))
          insertItems(namesWithStatus).transactAndRun

          persistence.update(updateAllActiveSql).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual 0
          }
        }
      }
  }

  "update" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
        prop { (active: Boolean) ⇒
          persistence.update(
            sql    = updateByStatusSql,
            values = (!active, active)
          ).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual 0
          }
        }
      }
    "return the number of affected rows equals to 1 after updating a single item in the table " in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data
        val id = insertItem(name = name, active = active).transactAndRun

        persistence.update(
          sql    = updateByIdSql,
          values = (name, !active, id)
        ).transactAndAttempt must be_\/-[Int].which {
          affectedRows ⇒
            affectedRows mustEqual 1
        }

        fetchItemById(id).transactAndAttempt must be_\/-[PersistenceItem].which {
          item ⇒
            item.id mustEqual id
            item.name mustEqual name
            item.active mustEqual !active
        }
      }
    }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
        prop { (names: List[String]) ⇒
          val namesWithStatus = names map ((_, true))
          insertItems(namesWithStatus).transactAndRun

          persistence.update(
            sql    = updateByStatusSql,
            values = (false, true)
          ).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows must be greaterThan 0
          }
        }
      }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
        prop { (names: List[String]) ⇒
          val namesWithStatus = names map ((_, false))
          insertItems(namesWithStatus).transactAndRun

          persistence.update(
            sql    = updateByStatusSql,
            values = (false, true)
          ).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual 0
          }
        }
      }
    "return the number of affected rows equals to 1 after inserting a new item in the table" in {
      prop { (name: String, active: Boolean) ⇒

        persistence.update(
          sql    = insertSql,
          values = (name, active)
        ).transactAndAttempt must be_\/-[Int].which {
          affectedRows ⇒
            affectedRows mustEqual 1
        }
      }
    }
  }

  "updateWithGeneratedKeys" should {
    "insert a new PersistenceItem into the table" in {
      prop { (data: (String, Boolean)) ⇒
        val (name, active) = data

        persistence.updateWithGeneratedKeys[Long](
          sql    = insertSql,
          fields = List("id"),
          values = data
        ).transactAndAttempt must be_\/-[Long].which {
            id ⇒
              fetchItemById(id).transactAndAttempt must be_\/-[PersistenceItem].which {
                item ⇒
                  item.id mustEqual id
                  item.name mustEqual name
                  item.active mustEqual active
              }
          }
      }
    }
  }

  "updateMany" should {
    "return the number of affected rows after inserting a batch of items in the table" in {
      prop { (data: List[(String, Boolean)]) ⇒

        persistence.updateMany(
          sql    = insertSql,
          values = data
        ).transactAndAttempt must be_\/-[Int].which {
          affectedRows ⇒
            affectedRows mustEqual data.size
        }
      }
    }

    "return the number of affected rows equals to 0 after updating a batch of items " +
      "in the table if the table is empty" in {
        prop { (data: List[(String, Boolean, Long)]) ⇒
          persistence.updateMany(
            sql    = updateByIdSql,
            values = data
          ).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual 0
          }
        }
      }
    "return the number of affected rows after updating a batch of items in the table " +
      "if the items exist" in {
        prop { (data: List[(String, Boolean)]) ⇒
          val fetchData = {
            for {
              _ ← insertItems(data)
              result ← fetchAll
            } yield result
          }.transactAndRun map {
            case (name, active, id) ⇒ (name, !active, id)
          }

          persistence.updateMany(
            sql    = updateByIdSql,
            values = fetchData
          ).transactAndAttempt must be_\/-[Int].which {
            affectedRows ⇒
              affectedRows mustEqual fetchData.size
          }
        }
      }
  }
}
