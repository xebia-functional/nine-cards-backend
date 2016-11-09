package cards.nine.services.persistence

import cards.nine.commons.config.Domain.DatabaseConfiguration
import cards.nine.commons.config.NineCardsConfig.nineCardsConfiguration
import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import doobie.imports.Transactor
import doobie.util.capture.Capture
import doobie.util.transactor.DriverManagerTransactor

import scalaz.concurrent.Task
import scalaz.{ Catchable, Monad }

class DatabaseTransactor[M[_]: Catchable: Capture](config: DatabaseConfiguration)(implicit MM: Monad[M]) {

  import MM.monadSyntax._

  def hikariTransactor: M[HikariTransactor[M]] =
    for {
      xa ← HikariTransactor[M](
        driverClassName = config.default.driver,
        url             = config.default.url,
        user            = config.default.user,
        pass            = config.default.password
      )
      _ ← xa.configure(hx ⇒
        MM.pure {
          hx.setMaximumPoolSize(config.hikari.maximumPoolSize)
          hx.setMaxLifetime(config.hikari.maxLifetime)
        })
    } yield xa

  def transactor: Transactor[M] =
    DriverManagerTransactor[M](
      driver = config.default.driver,
      url    = config.default.url,
      user   = config.default.user,
      pass   = config.default.password
    )
}

object DatabaseTransactor {

  implicit val transactor: HikariTransactor[Task] =
    new DatabaseTransactor[Task](nineCardsConfiguration.db)
      .hikariTransactor
      .unsafePerformSync
}
