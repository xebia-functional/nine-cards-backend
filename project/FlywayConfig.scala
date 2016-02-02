import com.typesafe.config._
import sbt.Keys._
import sbt._

object FlywayConfig {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  class CustomConfig(fileName: File) {

    val config = ConfigFactory.parseFile(fileName).resolve

    def envOrElseConfig(name: String): String = {
      sys.props.getOrElse(
        key = name,
        default = config.getString(name)
      )
    }
  }

  lazy val databaseConfig = settingKey[DatabaseConfig]("The database config to use in Flyway")

  lazy val databaseConfigDef: Def.Initialize[DatabaseConfig] =
    Def.setting[DatabaseConfig] {
      val config = new CustomConfig((resourceDirectory in Compile).value / "application.conf")
      val prefix = "db.default"

      DatabaseConfig(
        driver = config.envOrElseConfig(s"$prefix.driver"),
        url = config.envOrElseConfig(s"$prefix.url"),
        user = config.envOrElseConfig(s"$prefix.user"),
        password = config.envOrElseConfig(s"$prefix.password")
      )
    }
}