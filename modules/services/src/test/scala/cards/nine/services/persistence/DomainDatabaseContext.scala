package cards.nine.services.persistence

import java.sql.Connection

import cards.nine.commons.config.DummyConfig
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import doobie.contrib.postgresql.pgtypes._
import doobie.free.{ drivermanager ⇒ FD }
import doobie.imports._
import org.specs2.matcher.MatchResult
import shapeless.HNil

import scalaz.{ Foldable, \/ }
import scalaz.concurrent.Task
import scalaz.syntax.apply._

trait BasicDatabaseContext extends DummyConfig {

  val dbPrefix = "ninecards.db"

  class CustomTransactor[B](
    implicit
    beforeActions: ConnectionIO[B]
  ) extends Transactor[Task] {
    val driver = config.db.default.driver
    def url = config.db.default.url
    val user = config.db.default.user
    val pass = config.db.default.password

    val connect: Task[Connection] =
      Task.delay(Class.forName(driver)) *> FD.getConnection(url, user, pass).trans[Task]

    override val before = super.before <* beforeActions
  }

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

  implicit class Transacting[A](operation: ConnectionIO[A])(implicit transactor: Transactor[Task]) {
    def transactAndRun: A = operation.transact(transactor).unsafePerformSync

    def transactAndAttempt: \/[Throwable, A] = operation.transact(transactor).unsafePerformSyncAttempt
  }
}

trait PersistenceDatabaseContext extends BasicDatabaseContext {
  def createTable: ConnectionIO[Int] =
    sql"""
          CREATE TABLE IF NOT EXISTS persistence (
          id   BIGINT AUTO_INCREMENT,
          name VARCHAR NOT NULL,
          active BOOL NOT NULL)""".update.run

  implicit val before: ConnectionIO[Int] = createTable

  implicit val transactor: Transactor[Task] = new CustomTransactor
}

trait DomainDatabaseContext extends BasicDatabaseContext {

  val deviceToken: Option[String] = Option("d9f48907-0374-4b3a-89ec-433bd64de2e5")
  val emptyDeviceToken: Option[String] = None

  implicit val transactor: Transactor[Task] =
    DriverManagerTransactor[Task](
      driver = db.domain.driver,
      url    = db.domain.url,
      user   = db.domain.user,
      pass   = db.domain.password
    )

  val deleteAllRows = for {
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

  implicit val userPersistence = new Persistence[User](supportsSelectForUpdate = false)
  implicit val installationPersistence =
    new Persistence[Installation](supportsSelectForUpdate = false)
  implicit val collectionPersistence =
    new Persistence[SharedCollection](supportsSelectForUpdate = false)
  implicit val collectionSubscriptionPersistence =
    new Persistence[SharedCollectionSubscription](supportsSelectForUpdate = false)
  implicit val countryPersistence =
    new Persistence[Country](supportsSelectForUpdate = false)

  val collectionPersistenceServices = CollectionServices.services
  val countryPersistenceServices = CountryServices.services
  val subscriptionPersistenceServices = SubscriptionServices.services
  val userPersistenceServices = UserServices.services
}

