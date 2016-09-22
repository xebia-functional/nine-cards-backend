package cards.nine.processes.utils

import doobie.contrib.hikari.hikaritransactor.HikariTransactor

import scalaz.concurrent.Task

object DatabaseContext extends DummyNineCardsConfig {

  implicit val transactor: Task[HikariTransactor[Task]] = new DatabaseTransactor(config).transactor
}
