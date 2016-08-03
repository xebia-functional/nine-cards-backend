package com.fortysevendeg.ninecards.config

import com.typesafe.config.ConfigFactory

object NineCardsConfig {

  val config = ConfigFactory.load

  def getConfigValue(key: String) = sys.props.getOrElse(key, config.getString(key))

  def getOptionalConfigValue(key: String): Option[String] = config.hasPath(key) match {
    case true => Option (getConfigValue (key) ) map (_.trim) filterNot (_.isEmpty)
    case _ => None
  }

  def getConfigNumber(key: String) = {
    val str = getConfigValue(key)
    try {
      str.toInt
    } catch {
      case e: NumberFormatException =>
        throw new RuntimeException( s"Configuration value $str for key $key is not a well-formatted integer")
    }
  }
}
