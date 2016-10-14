package cards.nine.googleplay.service.free.interpreter.webscrapper

import cards.nine.commons.NineCardsConfig._

case class Configuration(
  protocol: String,
  host: String,
  port: Int,
  detailsPath: String
)

object Configuration {

  def load: Configuration = {
    def getApiConf(suff: String) = defaultConfig.getString(s"ninecards.google.play.storeweb.$suff")
    Configuration(
      protocol    = getApiConf("protocol"),
      host        = getApiConf("host"),
      port        = getApiConf("port").toInt,
      detailsPath = getApiConf("paths.details")
    )
  }

}