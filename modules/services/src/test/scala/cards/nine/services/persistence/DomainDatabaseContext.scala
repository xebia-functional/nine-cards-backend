/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.persistence

import cards.nine.commons.config.DummyConfig
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import doobie.contrib.postgresql.pgtypes._
import doobie.imports._
import org.specs2.matcher.MatchResult
import scalaz.concurrent.Task
import scalaz.{ Foldable, \/ }
import shapeless.HNil

trait BasicDatabaseContext extends DummyConfig {

  def insertItem[A: Composite](sql: String, values: A): ConnectionIO[Long] =
    Update[A](sql).toUpdate0(values).withUniqueGeneratedKeys[Long]("id")

  def insertItems[F[_]: Foldable, A: Composite](sql: String, values: F[A]): ConnectionIO[Int] =
    Update[A](sql).updateMany(values)

  def insertItemWithoutGeneratedKeys[A: Composite](sql: String, values: A): ConnectionIO[Int] =
    Update[A](sql).toUpdate0(values).run

  def deleteItems(sql: String): ConnectionIO[Int] = Update0(sql, None).run

  def getItem[A: Composite, B: Composite](sql: String, values: A): ConnectionIO[B] =
    Query[A, B](sql).unique(values)

  def getItems[A: Composite](sql: String): ConnectionIO[List[A]] =
    Query[HNil, A](sql, None).toQuery0(HNil).to[List]

  def getItems[A: Composite, B: Composite](sql: String, values: A): ConnectionIO[List[B]] =
    Query[A, B](sql).to[List](values)

  def getOptionalItem[A: Composite, B: Composite](sql: String, values: A): ConnectionIO[Option[B]] =
    Query[A, B](sql).option(values)

  def runDDLQuery(sql: String): ConnectionIO[Int] = Update0(sql, None).run

  implicit class Transacting[A](operation: ConnectionIO[A])(implicit transactor: Transactor[Task]) {
    def transactAndRun: A = transactor.trans(operation).unsafePerformSync

    def transactAndAttempt: \/[Throwable, A] = transactor.trans(operation).unsafePerformSyncAttempt
  }

  implicit val transactor: Transactor[Task] =
    DriverManagerTransactor[Task](
      driver = config.db.default.driver,
      url    = config.db.default.url,
      user   = config.db.default.user,
      pass   = config.db.default.password
    )
}

trait PersistenceDatabaseContext extends BasicDatabaseContext {

  val allFields = List("id", "name", "active")

  val deleteAllSql = "DELETE FROM persistence"
  val fetchAllSql = "SELECT id,name,active FROM persistence"
  val getAllSql = "SELECT name,active,id FROM persistence"
  val fetchAllActiveSql = "SELECT id,name,active FROM persistence WHERE active=true"
  val fetchByIdSql = "SELECT id,name,active FROM persistence WHERE id=?"
  val fetchByIdAndStatusSql = "SELECT id,name,active FROM persistence WHERE id=? AND active=?"
  val fetchByStatusSql = "SELECT id,name,active FROM persistence WHERE active=?"
  val insertSql = "INSERT INTO persistence (name,active) VALUES (?,?)"
  val updateAllSql = "UPDATE persistence SET active=false"
  val updateAllActiveSql = "UPDATE persistence SET active=false WHERE active=true"
  val updateByIdSql = "UPDATE persistence SET name=?,active=? WHERE id=?"
  val updateByStatusSql = "UPDATE persistence SET active=? WHERE active=?"
  val dropTableSql = "drop table if exists persistence"
  val createTableSql =
    """
      |create table if not exists persistence(
      |id bigserial not null primary key,
      |name character varying(256) not null,
      |active boolean not null)
    """.stripMargin

  case class PersistenceItem(id: Long, name: String, active: Boolean)

  case class PersistenceItemData(name: String, active: Boolean)

  val persistence = new Persistence[PersistenceItem]
}

trait DomainDatabaseContext extends BasicDatabaseContext {

  val deviceToken: Option[String] = Option("dddddddd-dddd-bbbb-bbbb-bbbbbbbbbbbb")
  val emptyDeviceToken: Option[String] = None

  val deleteAllRows: ConnectionIO[Unit] = for {
    _ ← deleteItems("delete from sharedcollectionsubscriptions")
    _ ← deleteItems("delete from sharedcollections")
    _ ← deleteItems("delete from installations")
    _ ← deleteItems("delete from users")
  } yield Unit

  object WithEmptyDatabase {
    def apply[A](check: ⇒ MatchResult[A]) = {
      deleteAllRows.transactAndRun
      check
    }
  }

  implicit val userPersistence = new Persistence[User]
  implicit val installationPersistence = new Persistence[Installation]
  implicit val collectionPersistence = new Persistence[SharedCollection]
  implicit val collectionSubscriptionPersistence = new Persistence[SharedCollectionSubscription]
  implicit val countryPersistence = new Persistence[Country]

  val collectionPersistenceServices = CollectionServices.services
  val countryPersistenceServices = CountryServices.services
  val subscriptionPersistenceServices = SubscriptionServices.services
  val userPersistenceServices = UserServices.services
}

