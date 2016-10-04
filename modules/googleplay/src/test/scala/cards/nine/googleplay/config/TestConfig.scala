package cards.nine.googleplay.config

import cards.nine.googleplay.config.NineCardsConfig.getConfigValue
import cards.nine.googleplay.domain._

trait TestConfig {

  lazy val token = Token(getConfigValue("test.token"))
  lazy val androidId = AndroidId(getConfigValue("test.androidId"))
  lazy val localization = Localization(getConfigValue("test.localization"))

  lazy val authParams = GoogleAuthParams(androidId, token, Some(localization))
}

object TestConfig extends TestConfig
