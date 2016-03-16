package com.fortysevendeg.ninecards.services

import com.fortysevendeg.ninecards.services.common.NineCardsConfig._
import doobie.imports._

import scalaz.Scalaz._
import scalaz.concurrent.Task

package object persistence {

  implicit lazy val transactor: Transactor[Task] = DriverManagerTransactor[Task](
    driver = getString("db.default.driver"),
    url = getString("db.default.url"),
    user = getString("db.default.user"),
    pass = getString("db.default.password"))

  implicit def toConnectionIO[T](t: T): ConnectionIO[T] = t.point[ConnectionIO]

  implicit def toConnectionIOMonad[F[_], T](ft: F[T]): ConnectionIO[F[T]] = ft.point[ConnectionIO]
}
