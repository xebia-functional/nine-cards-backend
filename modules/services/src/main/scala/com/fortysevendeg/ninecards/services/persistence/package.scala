package com.fortysevendeg.ninecards.services

import doobie.imports._

import scalaz.concurrent.Task

package object persistence {

  val transactor = DriverManagerTransactor[Task](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:ninecards",
    user = "postgres",
    pass = "postgres")

}
