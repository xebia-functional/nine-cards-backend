package com.fortysevendeg.ninecards.services

import com.fortysevendeg.ninecards.services.common.NineCardsConfig
import doobie.imports._

import scalaz.Scalaz._
import scalaz.concurrent.Task

package object persistence {

  implicit def transactor(implicit config: NineCardsConfig): Transactor[Task] =
    DriverManagerTransactor[Task](
      driver = config.getString("db.default.driver"),
      url    = config.getString("db.default.url"),
      user   = config.getString("db.default.user"),
      pass   = config.getString("db.default.password")
    )

  implicit def toConnectionIO[T](t: T): ConnectionIO[T] = t.point[ConnectionIO]

  implicit def toConnectionIOMonad[F[_], T](ft: F[T]): ConnectionIO[F[T]] = ft.point[ConnectionIO]
}
