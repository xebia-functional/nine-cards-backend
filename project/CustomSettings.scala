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
import com.typesafe.config.ConfigFactory
import sbt.Keys._
import sbt._

import scala.util.matching.Regex

object CustomSettings {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  lazy val databaseConfig = settingKey[DatabaseConfig]("The database config to use in Flyway")

  lazy val databaseConfigDef: Def.Initialize[DatabaseConfig] =
    Def.setting[DatabaseConfig] {
      val config = ConfigFactory.load()
      val prefix = "ninecards.db.default"

      def envOrElseConfig(name: String) = {
        sys.props.get(name) getOrElse {
          if (config.hasPath(name))
            config.getString(name)
          else {
            println(s"No configuration setting found for key '$name'")
            ""
          }
        }
      }

      val parseDatabaseConnectionUrl = {
        val protocol = "([^:\\/]+:\\/\\/)"
        val userInfo = "([^:]+)(:([^@]+))?@"
        val serverInstanceInfo = s"([^\\/:]+)(:\\d+)?(\\/.+)?"

        val DatabaseUrl: Regex = s"$protocol$userInfo($serverInstanceInfo)?".r

        envOrElseConfig(s"$prefix.url") match {
          case DatabaseUrl(_, userValue, _, passwordValue, urlValue, _, _, _) ⇒
            (urlValue, userValue, Option(passwordValue).getOrElse(""))
          case _ =>
            println(s"No valid value for key '$prefix.url'. Expected format: protocol://user[:password]@server/database")
            ("", "", "")
        }
      }

      val (url, user, password) = parseDatabaseConnectionUrl
      val urlPrefix = envOrElseConfig(s"$prefix.urlPrefix")

      DatabaseConfig(
        driver = envOrElseConfig(s"$prefix.driver"),
        url = s"$urlPrefix$url",
        user = user,
        password = password
      )
    }

  lazy val apiResourcesFolder = settingKey[File]("The resources folder for api project")

  lazy val apiResourcesFolderDef: Def.Initialize[File] =
    Def.setting[File] {
      (baseDirectory in LocalProject("api")).value / "src/main/resources"
    }
}