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
package cards.nine.commons.config

import cats.data.{ Validated, ValidatedNel }
import cats.implicits._

object ParserUtils {

  object uriregex {
    val schemeR = """[^:/]+"""
    val userR = """[^:]+"""
    val passwordR = "[^@]+"
    val hostR = """[^/:]+"""
    val portR = """\d+"""
    val pathR = """/.*"""
  }

  object database {
    import uriregex._

    val userInfo = s"($userR)?(?::($passwordR))?"

    val serverInstanceInfo = s"""(?:$hostR)(?::$portR)?(?:$pathR)?"""

    val DatabaseUrl = s"""$schemeR://(?:$userInfo@)?($serverInstanceInfo)?""".r

    case class PersistenceConnectionInfo(user: String, password: String, url: String)

    def parseConnectionString(raw: String): ValidatedNel[ConfigError, PersistenceConnectionInfo] =
      raw match {
        case DatabaseUrl(u, p, ur) ⇒

          val user = Option(u).toValidNel[ConfigError](MissingConfigValue("PostgreSQL user missing"))
          val password: ValidatedNel[ConfigError, String] = Validated.valid(Option(p).getOrElse(""))
          val url = Option(ur).toValidNel[ConfigError](MissingConfigValue("PostgreSQL URL missing"))

          (user |@| password |@| url) map (PersistenceConnectionInfo.apply _)

        case _ ⇒
          Validated.invalidNel(
            UnexpectedConnectionURL("Unexpected database connection URL: protocol://user[:password]@server/database")
          )
      }
  }

  object cache {

    import uriregex._

    val userInfo = s"""(?:$userR:)?($passwordR)?@"""

    val serverInstanceInfo = s"""($hostR):($portR)(?:$pathR)?"""

    val CacheUrl = s"""$schemeR://(?:$userInfo)?(?:$serverInstanceInfo)?""".r

    case class CacheConnectionInfo(secret: Option[String], host: String, port: Int)

    def parseConnectionString(raw: String): ValidatedNel[ConfigError, CacheConnectionInfo] =
      raw match {
        case CacheUrl(s, h, p) ⇒

          val secret: ValidatedNel[ConfigError, Option[String]] = Validated.valid(Option(s))
          val host = Option(h).toValidNel[ConfigError](MissingConfigValue("Cache host missing"))
          val port: ValidatedNel[ConfigError, Int] =
            Option(p)
              .toValidNel[ConfigError](MissingConfigValue("Cache port missing"))
              .andThen(validatedString2Int)

          (secret |@| host |@| port) map (CacheConnectionInfo.apply _)

        case _ ⇒
          Validated.invalidNel(
            UnexpectedConnectionURL("Unexpected cache connection URL: protocol://[[user:]secret@]server:port[/database]")
          )
      }
  }

}
