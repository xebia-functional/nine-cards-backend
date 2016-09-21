package cards.nine.services

import cards.nine.services.common.NineCardsConfig
import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import doobie.imports.ConnectionIO

import scalaz.Scalaz._
import scalaz.concurrent.Task
package object persistence {

  implicit def transactor(implicit config: NineCardsConfig): Task[HikariTransactor[Task]] =
    for {
      xa ← HikariTransactor[Task](
        driverClassName = config.getString("db.default.driver"),
        url             = config.getString("db.default.url"),
        user            = config.getString("db.default.user"),
        pass            = config.getString("db.default.password")
      )
      _ ← xa.configure(hx ⇒
        Task.delay(hx.setMaximumPoolSize(config.getInt("db.hikari.maximumPoolSize"))))
    } yield xa

  implicit def toConnectionIO[T](t: T): ConnectionIO[T] = t.point[ConnectionIO]
}
