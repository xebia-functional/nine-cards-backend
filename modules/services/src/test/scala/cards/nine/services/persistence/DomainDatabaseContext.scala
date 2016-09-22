package cards.nine.services.persistence

import cards.nine.services.free.domain
import cards.nine.services.free.domain._
import doobie.imports._
import org.flywaydb.core.Flyway

import scala.util.Random
import scalaz.{ \/, Foldable }
import scalaz.concurrent.Task

trait DomainDatabaseContext {

  import CustomComposite._

  val deviceToken: Option[String] = Option("d9f48907-0374-4b3a-89ec-433bd64de2e5")
  val emptyDeviceToken: Option[String] = None

  val testDriver = "org.h2.Driver"
  val testUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
  val testUsername = "sa"
  val testPassword = ""

  val transactor: Transactor[Task] = DriverManagerTransactor[Task](testDriver, testUrl, testUsername, testPassword)

  implicit class Transacting[A](operation: ConnectionIO[A]) {
    def transactAndRun: A = operation.transact(transactor).unsafePerformSync
    def transactAndAttempt: \/[Throwable, A] = operation.transact(transactor).unsafePerformSyncAttempt
  }

  implicit val userPersistence = new Persistence[User](supportsSelectForUpdate = false)
  implicit val installationPersistence = new Persistence[Installation](supportsSelectForUpdate = false)
  implicit val collectionPersistence = new Persistence[SharedCollection](supportsSelectForUpdate = false)
  implicit val collectionPackagePersistence = new Persistence[SharedCollectionPackage](supportsSelectForUpdate = false)
  implicit val collectionSubscriptionPersistence = new Persistence[SharedCollectionSubscription](supportsSelectForUpdate = false)
  implicit val rankingPersistence: Persistence[domain.rankings.Entry] =
    new Persistence[domain.rankings.Entry](supportsSelectForUpdate = false)
  val userPersistenceServices = new UserPersistenceServices
  val collectionPersistenceServices = new SharedCollectionPersistenceServices
  val scSubscriptionPersistenceServices = new SharedCollectionSubscriptionPersistenceServices
  val rankingPersistenceServices = new rankings.Services(rankingPersistence)

  val flywaydb = new Flyway
  flywaydb.setDataSource(testUrl, testUsername, testPassword)
  flywaydb.migrate()

  def insertItemWithoutGeneratedKeys[A: Composite](sql: String, values: A): ConnectionIO[Int] =
    Update[A](sql).toUpdate0(values).run

  def insertItem[A: Composite](sql: String, values: A): ConnectionIO[Long] =
    Update[A](sql).toUpdate0(values).withUniqueGeneratedKeys[Long]("id")

  def insertItems[F[_]: Foldable, A: Composite](sql: String, values: F[A]): ConnectionIO[Int] =
    Update[A](sql).updateMany(values)

  def deleteItems(sql: String): ConnectionIO[Int] = Update0(sql, None).run
}
