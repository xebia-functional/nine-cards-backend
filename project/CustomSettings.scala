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