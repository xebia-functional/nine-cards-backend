package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.googleplay.config.NineCardsConfig.getConfigValue

case class Configuration(
  protocol: String,
  host: String,
  port: Int,
  detailsPath: String
)

object Configuration {

  def load: Configuration = {
    def getApiConf(suff: String) = getConfigValue(s"ninecards.googleplay.storeweb.$suff")
    Configuration(
      protocol    = getApiConf("protocol"),
      host        = getApiConf("host"),
      port        = getApiConf("port").toInt,
      detailsPath = getApiConf("paths.details")
    )
  }

}