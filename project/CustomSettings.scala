import com.typesafe.config.ConfigFactory
import sbt.Keys._
import sbt._

object CustomSettings {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  lazy val databaseConfig = settingKey[DatabaseConfig]("The database config to use in Flyway")

  lazy val databaseConfigDef: Def.Initialize[DatabaseConfig] =
    Def.setting[DatabaseConfig] {
      val config = ConfigFactory.load()
      val prefix = "db.default"

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

      DatabaseConfig(
        driver = envOrElseConfig(s"$prefix.driver"),
        url = envOrElseConfig(s"$prefix.url"),
        user = envOrElseConfig(s"$prefix.user"),
        password = envOrElseConfig(s"$prefix.password")
      )
    }

  lazy val apiResourcesFolder = settingKey[File]("The resources folder for api project")

  lazy val apiResourcesFolderDef: Def.Initialize[File] =
    Def.setting[File] {
      (baseDirectory in LocalProject("api")).value / "src/main/resources"
    }
}