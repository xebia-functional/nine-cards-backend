package cards.nine.services.persistence

import cards.nine.commons.NineCardsConfig
import doobie.contrib.hikari.hikaritransactor.HikariTransactor
import doobie.imports.Transactor
import doobie.util.capture.Capture
import doobie.util.transactor.DriverManagerTransactor

import scalaz.concurrent.Task
import scalaz.{ Catchable, Monad }

class DatabaseTransactor[M[_]: Catchable: Capture](config: NineCardsConfig)(implicit MM: Monad[M]) {

  import MM.monadSyntax._

  def hikariTransactor: M[HikariTransactor[M]] =
    for {
      xa ← HikariTransactor[M](
        driverClassName = config.getString("db.default.driver"),
        url             = config.getString("db.default.url"),
        user            = config.getString("db.default.user"),
        pass            = config.getString("db.default.password")
      )
      _ ← xa.configure(hx ⇒
        MM.pure {
          hx.setMaximumPoolSize(config.getInt("db.hikari.maximumPoolSize"))
          hx.setMaxLifetime(config.getInt("db.hikari.maxLifetime"))
        })
    } yield xa

  def transactor: Transactor[M] =
    DriverManagerTransactor[M](
      driver = config.getString("db.default.driver"),
      url    = config.getString("db.default.url"),
      user   = config.getString("db.default.user"),
      pass   = config.getString("db.default.password")
    )
}

object DatabaseTransactor {

  implicit val transactor: HikariTransactor[Task] =
    new DatabaseTransactor[Task](NineCardsConfig.defaultConfig)
      .hikariTransactor
      .unsafePerformSync
}
