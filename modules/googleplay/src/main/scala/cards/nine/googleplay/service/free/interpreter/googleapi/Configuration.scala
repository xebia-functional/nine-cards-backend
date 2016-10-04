package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.commons.NineCardsConfig._

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
    def getApiConf(suff: String) = defaultConfig.getString(s"ninecards.googleplay.googleapi.$suff")
    Configuration(
      protocol            = getApiConf("protocol"),
      host                = getApiConf("host"),
      port                = getApiConf("port").toInt,
      detailsPath         = getApiConf("paths.details"),
      listPath            = getApiConf("paths.list"),
      recommendationsPath = getApiConf("paths.recommendations")
    )
  }

}