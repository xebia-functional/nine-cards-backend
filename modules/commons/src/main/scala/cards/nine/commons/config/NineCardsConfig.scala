package cards.nine.commons.config

import cards.nine.commons.config.Domain.NineCardsConfiguration
import com.typesafe.config.{ Config, ConfigFactory, ConfigValue, ConfigValueType }
import java.util.Map.Entry

class NineCardsConfig(hocon: Option[String] = None) {

  import ConfigOps._

  val config: Config = hocon.fold(ConfigFactory.load)(ConfigFactory.parseString)

  def getSysPropKeyAsBoolean(key: String): Option[Boolean] =
    sys.props.get(key).map(_.toBoolean)

  def getSysPropKeyAsInt(key: String): Option[Int] =
    sys.props.get(key).map(_.toInt)

  def getInt(key: String) = getSysPropKeyAsInt(key).getOrElse(config.getInt(key))

  def getOptionalInt(
    key: String
  ) = getSysPropKeyAsInt(key).fold(config.getOptionalInt(key))(Option(_))

  def getString(key: String) = sys.props.getOrElse(key, config.getString(key))

  def getOptionalString(
    key: String
  ) = sys.props.get(key).fold(config.getOptionalString(key))(Option(_))

  def getStringList(key: String): List[String] = {
    import scala.collection.JavaConversions._
    config.getStringList(key).toList
  }

  def getBoolean(key: String) = getSysPropKeyAsBoolean(key).getOrElse(config.getBoolean(key))

  def getOptionalBoolean(
    key: String
  ) = getSysPropKeyAsBoolean(key).fold(config.getOptionalBoolean(key))(Option(_))

  def getMap(key: String): Map[String, String] = {
    import collection.JavaConverters._
    def getStringValue(entry: Entry[String, ConfigValue]): Option[(String, String)] = {
      val value = entry.getValue()
      if (value.valueType() == ConfigValueType.STRING)
        Some(entry.getKey() → value.unwrapped().asInstanceOf[String])
      else None
    }

    def getEntry(entry: Entry[String, ConfigValue]): (String, String) =
      entry.getKey() → entry.getValue().render()

    config.getConfig(key).entrySet().asScala.toList.flatMap(getStringValue).toMap
  }

}

object ConfigOps {

  implicit class Ops(val config: Config) {

    def getOptionalValue[T](path: String)(f: String ⇒ T) =
      if (config.hasPath(path)) {
        Option(f(path))
      } else {
        None
      }

    def getOptionalBoolean(path: String): Option[Boolean] = getOptionalValue(path)(config.getBoolean)

    def getOptionalInt(path: String): Option[Int] = getOptionalValue(path)(config.getInt)

    def getOptionalString(path: String): Option[String] = getOptionalValue(path)(config.getString)
  }

}

object NineCardsConfig {

  val defaultConfig: NineCardsConfig = new NineCardsConfig

  implicit val nineCardsConfiguration: NineCardsConfiguration = NineCardsConfiguration(defaultConfig)
}
