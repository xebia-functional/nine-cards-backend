package com.fortysevendeg.ninecards.services

import doobie.imports._

import scalaz.concurrent.Task

package object persistence {

  val transactor = DriverManagerTransactor[Task]("org.postgresql.Driver", "jdbc:postgresql:ninecards", "postgres", "postgres")

}
