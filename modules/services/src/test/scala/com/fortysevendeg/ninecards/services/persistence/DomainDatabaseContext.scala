package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import doobie.imports._
import org.flywaydb.core.Flyway
import scala.util.Random
import scalaz.concurrent.Task


trait DomainDatabaseContext {

  val testDriver = "org.h2.Driver"
  val testUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
  val testUsername = "sa"
  val testPassword = ""

  val transactor: Transactor[Task] = DriverManagerTransactor[Task](testDriver, testUrl, testUsername, testPassword)

  implicit val userPersistenceImpl = new PersistenceImpl[User](supportsSelectForUpdate = false)
  implicit val installationPersistenceImpl = new PersistenceImpl[Installation](supportsSelectForUpdate = false)
  val userPersistenceServices = new UserPersistenceServices

  val flywaydb = new Flyway
  flywaydb.setDataSource(testUrl, testUsername, testPassword)
  flywaydb.migrate()

}
