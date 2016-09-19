package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue

case class Configuration(
  protocol: String,
  host: String,
  port: Int,
  detailsPath: String,
  listPath: String,
  recommendationsPath: String
)

object Configuration {

  def load(): Configuration = {
    def getApiConf(suff: String) = getConfigValue(s"ninecards.googleplay.googleapi.$suff")
    Configuration(
      protocol = getApiConf("protocol"),
      host = getApiConf("host"),
      port = getApiConf("port").toInt,
      detailsPath = getApiConf("paths.details"),
      listPath = getApiConf("paths.list"),
      recommendationsPath = getApiConf("paths.recommendations")
    )
  }

}