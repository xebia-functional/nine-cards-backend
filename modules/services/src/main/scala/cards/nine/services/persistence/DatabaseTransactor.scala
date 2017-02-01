/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
