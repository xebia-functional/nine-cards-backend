package com.fortysevendeg.ninecards.services

import com.fortysevendeg.ninecards.services.common.NineCardsConfig._
import doobie.imports._

import scalaz.concurrent.Task

package object persistence {

  val transactor = DriverManagerTransactor[Task](
    driver = getConfigValue("db.default.driver"),
    url = getConfigValue("db.default.url"),
    user = getConfigValue("db.default.user"),
    pass = getConfigValue("db.default.password"))

}
