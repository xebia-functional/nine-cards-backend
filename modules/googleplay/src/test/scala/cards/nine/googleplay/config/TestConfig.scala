package cards.nine.googleplay.config

import cards.nine.commons.NineCardsConfig._
import cards.nine.googleplay.domain._

trait TestConfig {

  lazy val token = Token(defaultConfig.getString("test.token"))
  lazy val androidId = AndroidId(defaultConfig.getString("test.androidId"))
  lazy val localization = Localization(defaultConfig.getString("test.localization"))

  lazy val authParams = GoogleAuthParams(androidId, token, Some(localization))
}

object TestConfig extends TestConfig
