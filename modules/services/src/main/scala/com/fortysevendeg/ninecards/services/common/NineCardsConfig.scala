package com.fortysevendeg.ninecards.services.common

import com.typesafe.config.{Config, ConfigFactory}

object NineCardsConfig {

  implicit class ConfigOps(val config: Config) {

    def getOptionalValue[T](path: String)(f: String => T) =
      if (config.hasPath(path)) {
        Option(f(path))
      } else {
        None
      }

    def getOptionalInt(path: String): Option[Int] = getOptionalValue(path)(config.getInt)

    def getOptionalString(path: String): Option[String] = getOptionalValue(path)(config.getString)
  }

  val config = ConfigFactory.load

  def getInt(key: String) = sys.props.getOrElse(key, config.getInt(key))

  def getOptionalInt(
    key: String) = sys.props.get(key).fold(config.getOptionalInt(key))(i => Option(i.toInt))

  def getString(key: String) = sys.props.getOrElse(key, config.getString(key))

  def getOptionalString(
    key: String) = sys.props.get(key).fold(config.getOptionalString(key))(Option(_))
}
