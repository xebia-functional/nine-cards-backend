package com.fortysevendeg.ninecards.services.common

import com.typesafe.config.ConfigFactory

object NineCardsConfig {

  val config = ConfigFactory.load

  def getConfigValue(key: String) = sys.props.getOrElse(key, config.getString(key))
}
