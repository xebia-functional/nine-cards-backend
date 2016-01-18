package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2._
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

import scalaz.concurrent.Task
import scalaz.std.iterable._

trait DatabaseContext {

  def trx = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1", "sa", "")

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
    active: Boolean): ConnectionIO[Long] =
    sql"INSERT INTO persistence (name, active) VALUES ($name,$active)".update.withUniqueGeneratedKeys[Long]("id")

  def insertItems(
    values: List[(String, Boolean)]): ConnectionIO[Int] =
    Update[(String, Boolean)]("INSERT INTO persistence (name, active) VALUES (?,?)").updateMany(values)

  def fetchAll: ConnectionIO[List[(String, Boolean, Long)]] =
    sql"SELECT name,active,id FROM persistence".query[(String, Boolean, Long)].list

  def fetchItemById(
    id: Long): ConnectionIO[PersistenceItem] =
    sql"SELECT id,name,active FROM persistence WHERE id=$id".query[PersistenceItem].unique

  def fetchItemByStatus(
    active: Boolean): ConnectionIO[List[PersistenceItem]] =
    sql"SELECT id,name,active FROM persistence WHERE active=$active".query[PersistenceItem].list

  def fetchItemByStatuses(
    active: Boolean): ConnectionIO[List[PersistenceItem]] = {
    val inactive = !active
    sql"SELECT id,name,active FROM persistence WHERE active=$active OR active=$inactive".query[PersistenceItem].list
  }

  val persistenceImpl = new PersistenceImpl

  def genBoundedList[T](minSize: Int = 1, maxSize: Int = 100, gen: Gen[T]): Gen[List[T]] =
    Gen.choose(minSize, maxSize) flatMap { size => Gen.listOfN(size, gen) }

  implicit val dataWithId: Arbitrary[List[(Long, String, Boolean)]] =
    Arbitrary(
      genBoundedList(
        minSize = 2,
        maxSize = 10,
        gen = Gen.resultOf((l: Long, s: String, b: Boolean) => (l, s, b))))

  implicit val data: Arbitrary[List[(String, Boolean)]] =
    Arbitrary(genBoundedList(
      minSize = 2,
      maxSize = 10,
      gen = Gen.resultOf((s: String, b: Boolean) => (s, b))))

  implicit val stringList: Arbitrary[List[String]] =
    Arbitrary(genBoundedList(minSize = 2, gen = Gen.resultOf((s: String) => s)))
}

class PersistenceImplSpec
  extends Specification
    with BeforeEach
    with DatabaseContext
    with DisjunctionMatchers
    with ScalaCheck {

  sequential

  override def before = {
    for {
      _ <- dropTable
      _ <- createTable
    } yield ()
  }.transact(trx).run

  "fetchList (SQL without parameters)" should {
    "return an empty list if the table is empty" in {
      prop { (i: Int) =>
        val list = persistenceImpl.fetchList[PersistenceItem](
          sql = fetchAllSql).transact(trx).run

        list must beEmpty
      }
    }

    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
      prop { (data: List[(String, Boolean)]) =>
        insertItems(data).transact(trx).run

        val list = persistenceImpl.fetchList[PersistenceItem](
          sql = fetchAllSql).transact(trx).run

        list must not be empty
      }
    }

    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, false))
        insertItems(namesWithStatus).transact(trx).run

        val list = persistenceImpl.fetchList[PersistenceItem](
          sql = fetchAllActiveSql).transact(trx).run

        list must beEmpty
      }
    }
  }

  "fetchList" should {
    "return an empty list if the table is empty" in {
      prop { (status: Boolean) =>
        val list = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = status).transact(trx).run

        list must beEmpty
      }
    }
    "return a list of PersistenceItem if there are some elements in the table that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        val list: List[PersistenceItem] = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = true).transact(trx).run

        list must not be empty
        list.forall(item => item.active) must beTrue
      }
    }
    "return an empty list if there aren't any elements in the table that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        val list: List[PersistenceItem] = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = false).transact(trx).run

        list must beEmpty
      }
    }
  }

  "fetchOption" should {
    "return None if the table is empty" in {
      prop { (status: Boolean) =>
        val persistenceItem = persistenceImpl.fetchOption[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = status).transact(trx).run

        persistenceItem must beEmpty
      }
    }
    "return a PersistenceItem if there is an element in the table that meets the criteria" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run

        val persistenceItem = persistenceImpl.fetchOption[(Long, Boolean), PersistenceItem](
          sql = fetchByIdAndStatusSql,
          values = (id, active)).transact(trx).run

        persistenceItem must beSome[PersistenceItem].which {
          item =>
            item.id mustEqual id
            item.name mustEqual name
            item.active mustEqual active
        }
      }
    }
    "return None if there isn't any element in the table that meets the criteria" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run
        val persistenceItem = persistenceImpl.fetchOption[(Long, Boolean), PersistenceItem](
          sql = fetchByIdAndStatusSql,
          values = (id, !active)).transact(trx).run

        persistenceItem must beEmpty
      }
    }
    "throw an exception if there are more than one element in the table that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.fetchOption[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = true).transact(trx).run must throwA[Throwable]
      }
    }
  }

  "fetchUnique" should {
    "throw an exception if the table is empty" in {
      prop { (id: Long) =>
        persistenceImpl.fetchUnique[Long, PersistenceItem](
          sql = fetchByIdSql,
          values = id).transact(trx).attemptRun must be_-\/[Throwable]
      }
    }
    "return a PersistenceItem if there is an element in the table with the given id" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run
        val item = persistenceImpl.fetchUnique[Long, PersistenceItem](
          sql = fetchByIdSql,
          values = id).transact(trx).run

        item.id mustEqual id
        item.name mustEqual name
      }
    }
    "throw an exception if there isn't any element in the table that meet the criteria" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run

        persistenceImpl.fetchUnique[(Long, Boolean), PersistenceItem](
          sql = fetchByIdAndStatusSql,
          values = (id, !active)).transact(trx).attemptRun must be_-\/[Throwable]
      }
    }
    "throw an exception if there are more than one element in the table that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.fetchUnique[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = true).transact(trx).run must throwA[Throwable]
      }
    }
  }

  "update (SQL without parameters)" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
      prop { (i: Int) =>
        persistenceImpl.update(sql = updateAllSql).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 0
        }
      }
    }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.update(updateAllActiveSql).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows must be greaterThan 0
        }
      }
    }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, false))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.update(updateAllActiveSql).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 0
        }
      }
    }
  }

  "update" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
      prop { (active: Boolean) =>
        persistenceImpl.update(
          sql = updateByStatusSql,
          values = (!active, active)).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 0
        }
      }
    }
    "return the number of affected rows equals to 1 after updating a single item in the table " in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run

        persistenceImpl.update(
          sql = updateByIdSql,
          values = (name, !active, id)).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 1
        }

        fetchItemById(id).transact(trx).attemptRun must be_\/-[PersistenceItem].which {
          item =>
            item.id mustEqual id
            item.name mustEqual name
            item.active mustEqual !active
        }
      }
    }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, true))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.update(
          sql = updateByStatusSql,
          values = (false, true)).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows must be greaterThan 0
        }
      }
    }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
      prop { (names: List[String]) =>
        val namesWithStatus = names map ((_, false))
        insertItems(namesWithStatus).transact(trx).run

        persistenceImpl.update(
          sql = updateByStatusSql,
          values = (false, true)).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 0
        }
      }
    }
    "return the number of affected rows equals to 1 after inserting a new item in the table" in {
      prop { (name: String, active: Boolean) =>

        persistenceImpl.update(
          sql = insertSql,
          values = (name, active)).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 1
        }
      }
    }
  }

  "updateWithGeneratedKeys" should {
    "insert a new PersistenceItem into the table" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data

        persistenceImpl.updateWithGeneratedKeys[(String, Boolean), Long](
          sql = insertSql,
          fields = List("id"),
          values = data).transact(trx).attemptRun must be_\/-[Long].which {
          id =>
            fetchItemById(id).transact(trx).attemptRun must be_\/-[PersistenceItem].which {
              item =>
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
      prop { (data: List[(String, Boolean)]) =>

        persistenceImpl.updateMany[List, (String, Boolean)](
          sql = insertSql,
          values = data).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual data.size
        }
      }
    }

    "return the number of affected rows equals to 0 after updating a batch of items " +
      "in the table if the table is empty" in {
      prop { (data: List[(String, Boolean, Long)]) =>
        persistenceImpl.updateMany[List, (String, Boolean, Long)](
          sql = updateByIdSql,
          values = data).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual 0
        }
      }
    }
    "return the number of affected rows after updating a batch of items in the table " +
      "if the items exist" in {
      prop { (data: List[(String, Boolean)]) =>
        val fetchData = {
          for {
            _ <- insertItems(data)
            result <- fetchAll
          } yield result
        }.transact(trx).run map {
          case (name, active, id) => (name, !active, id)
        }

        persistenceImpl.updateMany[List, (String, Boolean, Long)](
          sql = updateByIdSql,
          values = fetchData).transact(trx).attemptRun must be_\/-[Int].which {
          affectedRows =>
            affectedRows mustEqual fetchData.size
        }
      }
    }
  }
}
