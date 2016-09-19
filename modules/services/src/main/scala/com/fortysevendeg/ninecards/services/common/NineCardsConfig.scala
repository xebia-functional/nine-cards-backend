package com.fortysevendeg.ninecards.services.common

import com.fortysevendeg.ninecards.services.common.NineCardsConfig._
import com.typesafe.config.{ Config, ConfigFactory }

class NineCardsConfig(hocon: Option[String] = None) {

  val config = hocon.fold(ConfigFactory.load)(ConfigFactory.parseString)

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

  def getBoolean(key: String) = getSysPropKeyAsBoolean(key).getOrElse(config.getBoolean(key))

  def getOptionalBoolean(
    key: String
  ) = getSysPropKeyAsBoolean(key).fold(config.getOptionalBoolean(key))(Option(_))
}

object NineCardsConfig {

  implicit class ConfigOps(val config: Config) {

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

  implicit val defaultConfig: NineCardsConfig = new NineCardsConfig
}
