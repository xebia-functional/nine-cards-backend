package cards.nine.processes.utils

import cards.nine.services.common.NineCardsConfig
import doobie.contrib.hikari.hikaritransactor.HikariTransactor

import scalaz.concurrent.Task

class DatabaseTransactor(config: NineCardsConfig) {
  val transactor: Task[HikariTransactor[Task]] =
    for {
      xa ← HikariTransactor[Task](
        driverClassName = config.getString("db.default.driver"),
        url             = config.getString("db.default.url"),
        user            = config.getString("db.default.user"),
        pass            = config.getString("db.default.password")
      )
      _ ← xa.configure(hx ⇒
        Task.delay {
          hx.setMaximumPoolSize(config.getInt("db.hikari.maximumPoolSize"))
          hx.setMaxLifetime(config.getInt("db.hikari.maxLifetime"))
        })
    } yield xa
}

object DatabaseTransactor {

  implicit val trx: Task[HikariTransactor[Task]] =
    new DatabaseTransactor(NineCardsConfig.defaultConfig).transactor
}
