package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import doobie.imports._
import org.flywaydb.core.Flyway

import scala.util.Random
import scalaz.concurrent.Task


trait DomainDatabaseContext {

  val emptyDeviceToken: Option[String] = None

  val testDriver = "org.h2.Driver"
  val testUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
  val testUsername = "sa"
  val testPassword = ""

  val transactor: Transactor[Task] = DriverManagerTransactor[Task](testDriver, testUrl, testUsername, testPassword)

  implicit val userPersistence = new Persistence[User](supportsSelectForUpdate = false)
  implicit val installationPersistence = new Persistence[Installation](supportsSelectForUpdate = false)
  val userPersistenceServices = new UserPersistenceServices

  val flywaydb = new Flyway
  flywaydb.setDataSource(testUrl, testUsername, testPassword)
  flywaydb.migrate()

  def insertItem[A](
    sql: String,
    values: A)(implicit ev: Composite[A]): ConnectionIO[Long] =
    Update[A](sql).toUpdate0(values).withUniqueGeneratedKeys[Long]("id")
}
