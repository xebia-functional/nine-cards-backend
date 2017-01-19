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
import shapeless.HNil

import scalaz.concurrent.Task
import scalaz.{ Foldable, \/ }

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
    def transactAndRun: A = operation.transact(transactor).unsafePerformSync

    def transactAndAttempt: \/[Throwable, A] = operation.transact(transactor).unsafePerformSyncAttempt
  }

  implicit val transactor: Transactor[Task] =
    DriverManagerTransactor[Task](
      driver = db.default.driver,
      url    = db.default.url,
      user   = db.default.user,
      pass   = db.default.password
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

  val deviceToken: Option[String] = Option("d9f48907-0374-4b3a-89ec-433bd64de2e5")
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

