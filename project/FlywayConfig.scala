import com.typesafe.config.ConfigFactory
import sbt.{Def, settingKey}

object FlywayConfig {

  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  lazy val databaseConfig = settingKey[DatabaseConfig]("The database config to use in Flyway")

  lazy val databaseConfigDef: Def.Initialize[DatabaseConfig] =
    Def.setting[DatabaseConfig] {
      val config = ConfigFactory.load()
      val prefix = "db.default"

      def envOrElseConfig(name: String) = sys.props.get(name) getOrElse config.getString(name)

      DatabaseConfig(
        driver = envOrElseConfig(s"$prefix.driver"),
        url = envOrElseConfig(s"$prefix.url"),
        user = envOrElseConfig(s"$prefix.user"),
        password = envOrElseConfig(s"$prefix.password")
      )
    }
}