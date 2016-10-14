package cards.nine.services.persistence

import java.sql.Connection

import cards.nine.commons.NineCardsConfig
import cards.nine.services.free.domain
import cards.nine.services.free.domain._
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.ranking.{ Services ⇒ RankingServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import cards.nine.services.utils.DummyNineCardsConfig
import doobie.free.{ drivermanager ⇒ FD }
import doobie.imports._
import org.flywaydb.core.Flyway
import shapeless.HNil

import scalaz.{ Foldable, \/ }
import scalaz.concurrent.Task
import scalaz.syntax.apply._

trait BasicDatabaseContext extends DummyNineCardsConfig {

  val dbPrefix = "ninecards.db"

  class CustomTransactor[B](
    implicit
    beforeActions: ConnectionIO[B],
    config: NineCardsConfig
  ) extends Transactor[Task] {
    val driver = config.getString(s"$dbPrefix.default.driver")
    def url = config.getString(s"$dbPrefix.default.url")
    val user = config.getString(s"$dbPrefix.default.user")
    val pass = config.getString(s"$dbPrefix.default.password")

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

  import CustomComposite._

  val deviceToken: Option[String] = Option("d9f48907-0374-4b3a-89ec-433bd64de2e5")
  val emptyDeviceToken: Option[String] = None

  implicit val transactor: Transactor[Task] =
    DriverManagerTransactor[Task](
      driver = dummyConfig.getString(s"$dbPrefix.domain.driver"),
      url    = dummyConfig.getString(s"$dbPrefix.domain.url"),
      user   = dummyConfig.getString(s"$dbPrefix.domain.user"),
      pass   = dummyConfig.getString(s"$dbPrefix.domain.password")
    )

  val flywaydb = new Flyway

  flywaydb.setDataSource(
    dummyConfig.getString(s"$dbPrefix.domain.url"),
    dummyConfig.getString(s"$dbPrefix.domain.user"),
    dummyConfig.getString(s"$dbPrefix.domain.password")
  )

  flywaydb.migrate()

  implicit val userPersistence = new Persistence[User](supportsSelectForUpdate = false)
  implicit val installationPersistence =
    new Persistence[Installation](supportsSelectForUpdate = false)
  implicit val collectionPersistence =
    new Persistence[SharedCollection](supportsSelectForUpdate = false)
  implicit val collectionPackagePersistence =
    new Persistence[SharedCollectionPackage](supportsSelectForUpdate = false)
  implicit val collectionSubscriptionPersistence =
    new Persistence[SharedCollectionSubscription](supportsSelectForUpdate = false)
  implicit val countryPersistence =
    new Persistence[Country](supportsSelectForUpdate = false)
  implicit val rankingPersistence: Persistence[domain.rankings.Entry] =
    new Persistence[domain.rankings.Entry](supportsSelectForUpdate = false)

  val collectionPersistenceServices = CollectionServices.services
  val countryPersistenceServices = CountryServices.services
  val rankingPersistenceServices = RankingServices.services
  val subscriptionPersistenceServices = SubscriptionServices.services
  val userPersistenceServices = UserServices.services
}

