package com.fortysevendeg.ninecards.config

import com.typesafe.config.ConfigFactory

object NineCardsConfig {

  val config = ConfigFactory.load

  def getConfigValue(key: String) = sys.props.getOrElse(key, config.getString(key))
}
