package cards.nine.googleplay.config

import com.typesafe.config.ConfigFactory

object NineCardsConfig {

  val config = ConfigFactory.load

  def getConfigValue(key: String) = sys.props.getOrElse(key, config.getString(key))

  def getOptionalConfigValue(key: String): Option[String] =
    if (config.hasPath(key)) Option(getConfigValue(key)) map (_.trim) filterNot (_.isEmpty)
    else None

  def getConfigNumber(key: String) = {
    val str = getConfigValue(key)
    try {
      str.toInt
    } catch {
      case e: NumberFormatException ⇒
        throw new RuntimeException(s"Configuration value $str for key $key is not a well-formatted integer")
    }
  }
}
