package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import org.scalacheck.Gen
import org.specs2._
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.specification.BeforeEach

import scalaz.concurrent.Task
import scalaz.std.iterable._

trait DatabaseContext {

  def trx = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1", "sa", "")

  case class PersistenceItem(id: Long, name: String, active: Boolean)

  val insertSql = "INSERT INTO persistence (name,active) VALUES (?,?)"
  val fetchByIdSql = "SELECT id,name,active FROM persistence WHERE id=?"
  val fetchByIdAndStatusSql = "SELECT id,name,active FROM persistence WHERE id=? AND active=?"
  val fetchByStatusSql = "SELECT id,name,active FROM persistence WHERE active=?"

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
    values: List[(String, Boolean)]): ConnectionIO[List[Long]] =
    Update[(String, Boolean)]("INSERT INTO persistence (name, active) VALUES (?,?)").updateManyWithGeneratedKeys[Long]("id")(values).list

  def fetchItemById(
    id: Long): ConnectionIO[PersistenceItem] =
    sql"SELECT id,name,active FROM persistence WHERE id=$id".query[PersistenceItem].unique

  def fetchItemByStatus(
    active: Boolean): ConnectionIO[List[PersistenceItem]] =
    sql"SELECT id,name,active FROM persistence WHERE active=$active".query[PersistenceItem].list

  val persistenceImpl = new PersistenceImpl

  def genPersistenceItemData =
    for {
      name <- Gen.alphaStr
      active <- Gen.oneOf(true, false)
    } yield (name, active)
}

class TestSpec extends mutable.Specification with DatabaseContext with BeforeEach with ScalaCheck with DisjunctionMatchers {

  sequential

  override def before = {
    for {
      _ <- dropTable
      _ <- createTable
    } yield ()
  }.transact(trx).run

  "fetchList" should {
    "return an empty list if the table is empty" in {
      prop { (status: Boolean) =>
        val list = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = status).transact(trx).run

        list must beEmpty
      }.setGen(Gen.oneOf(true, false))
    }
    "return a list of PersistenceItem if there are some elements in the table that meet the criteria" in {
      prop { (name: String) =>
        val id = insertItem(name = name, active = true).transact(trx).run

        val list: List[PersistenceItem] = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = true).transact(trx).run

        list.forall(item => item.active) must beTrue
        list must not be empty
      }.setGen(Gen.alphaStr)
    }
    "return an empty list if there aren't any elements in the table that meet the criteria" in {
      prop { (name: String) =>
        val id = insertItem(name = name, active = true).transact(trx).run

        val list: List[PersistenceItem] = persistenceImpl.fetchList[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = false).transact(trx).run

        list must beEmpty
      }.setGen(Gen.alphaStr)
    }
  }

  "fetchOption" should {
    "return None if the table is empty" in {
      prop { (status: Boolean) =>
        val persistenceItem = persistenceImpl.fetchOption[Boolean, PersistenceItem](
          sql = fetchByStatusSql,
          values = status).transact(trx).run

        persistenceItem must beEmpty
      }.setGen(Gen.oneOf(true, false))
    }
    "return a PersistenceItem if there is an element in the table that meet the criteria" in {
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
      }.setGen(genPersistenceItemData)
    }
    "return None if there isn't any element in the table that meet the criteria" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run
        val persistenceItem = persistenceImpl.fetchOption[(Long, Boolean), PersistenceItem](
          sql = fetchByIdAndStatusSql,
          values = (id, !active)).transact(trx).run

        persistenceItem must beEmpty
      }.setGen(genPersistenceItemData)
    }
  }

  "fetchUnique" should {
    "throw an exception if the table is empty" in {
      prop { (id: Long) =>
        persistenceImpl.fetchUnique[Long, PersistenceItem](
          sql = fetchByIdSql,
          values = id).transact(trx).attemptRun must be_-\/[Throwable]
      }.setGen(Gen.choose(0, Long.MaxValue))
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
      }.setGen(genPersistenceItemData)
    }
    "throw an exception if there isn't any element in the table that meet the criteria" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data
        val id = insertItem(name = name, active = active).transact(trx).run

        persistenceImpl.fetchUnique[(Long, Boolean), PersistenceItem](
          sql = fetchByIdAndStatusSql,
          values = (id, !active)).transact(trx).attemptRun must be_-\/[Throwable]
      }.setGen(genPersistenceItemData)
    }
  }

  "update" should {
    "add a PersistenceItem into the table" in {
      prop { (data: (String, Boolean)) =>
        val (name, active) = data

        persistenceImpl.update[(String, Boolean), Long](
          sql = insertSql,
          fields = Seq("id"),
          values = data).transact(trx).attemptRun must be_\/-[Long].which {
          id =>
            fetchItemById(id).transact(trx).attemptRun must be_\/-[PersistenceItem].which {
              item =>
                item.id mustEqual id
                item.name mustEqual name
                item.active mustEqual active
            }
        }
      }.setGen(genPersistenceItemData)
    }

  }

  "updateMany" should {
    "add a batch of PersistenceItem into the table" in {
      prop { (names: Seq[String]) =>
        val nameAndStatus = names map ((_, true))

        persistenceImpl.updateMany[Seq, (String, Boolean), Long](
          sql = insertSql,
          fields = Seq("id"),
          values = nameAndStatus).transact(trx).attemptRun must be_\/-[Seq[Long]]

      }.setGen(Gen.nonEmptyListOf(Gen.alphaStr))
    }.pendingUntilFixed("Throws a Terminated(End) exception")

  }
}
