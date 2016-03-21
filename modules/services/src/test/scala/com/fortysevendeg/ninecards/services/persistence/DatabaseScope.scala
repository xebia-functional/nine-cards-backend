package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import org.flywaydb.core.Flyway
import org.joda.time.DateTime
import org.specs2.specification.Scope

import scalaz.concurrent.Task

trait DatabaseScope extends Scope {

  val databaseUrl = s"jdbc:h2:mem:test-${DateTime.now.getMillis};DB_CLOSE_DELAY=-1"

  val flywaydb = new Flyway
  flywaydb.setDataSource(databaseUrl, "sa", "")

  val trx = DriverManagerTransactor[Task](
    driver = "org.h2.Driver",
    url = databaseUrl,
    user = "sa",
    pass = "")

}
