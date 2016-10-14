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

  val dbPrefix = "ninecards.db"

  def hikariTransactor: M[HikariTransactor[M]] =
    for {
      xa ← HikariTransactor[M](
        driverClassName = config.getString(s"$dbPrefix.default.driver"),
        url             = config.getString(s"$dbPrefix.default.url"),
        user            = config.getString(s"$dbPrefix.default.user"),
        pass            = config.getString(s"$dbPrefix.default.password")
      )
      _ ← xa.configure(hx ⇒
        MM.pure {
          hx.setMaximumPoolSize(config.getInt(s"$dbPrefix.hikari.maximumPoolSize"))
          hx.setMaxLifetime(config.getInt(s"$dbPrefix.hikari.maxLifetime"))
        })
    } yield xa

  def transactor: Transactor[M] =
    DriverManagerTransactor[M](
      driver = config.getString(s"$dbPrefix.default.driver"),
      url    = config.getString(s"$dbPrefix.default.url"),
      user   = config.getString(s"$dbPrefix.default.user"),
      pass   = config.getString(s"$dbPrefix.default.password")
    )
}

object DatabaseTransactor {

  implicit val transactor: HikariTransactor[Task] =
    new DatabaseTransactor[Task](NineCardsConfig.defaultConfig)
      .hikariTransactor
      .unsafePerformSync
}
