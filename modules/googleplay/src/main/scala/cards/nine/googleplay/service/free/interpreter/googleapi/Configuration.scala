package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.commons.NineCardsConfig._

case class Configuration(
  protocol: String,
  host: String,
  port: Int,
  bulkDetailsPath: String,
  detailsPath: String,
  listPath: String,
  searchPath: String,
  recommendationsPath: String
)

object Configuration {

  def load(): Configuration = {
    def getApiConf(suff: String) = defaultConfig.getString(s"ninecards.google.play.api.$suff")
    Configuration(
      protocol            = getApiConf("protocol"),
      host                = getApiConf("host"),
      port                = getApiConf("port").toInt,
      bulkDetailsPath     = getApiConf("paths.bulkDetails"),
      detailsPath         = getApiConf("paths.details"),
      listPath            = getApiConf("paths.list"),
      searchPath          = getApiConf("paths.search"),
      recommendationsPath = getApiConf("paths.recommendations")
    )
  }

}