package com.fortysevendeg.ninecards.services

import doobie.imports._

import scalaz.concurrent.Task

package object persistence {

  val transactor = DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1", "sa", "")

}
