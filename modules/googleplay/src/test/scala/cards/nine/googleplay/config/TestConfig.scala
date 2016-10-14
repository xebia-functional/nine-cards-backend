package cards.nine.googleplay.config

import cards.nine.commons.NineCardsConfig._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }

trait TestConfig {

  lazy val token = MarketToken(defaultConfig.getString("ninecards.test.token"))
  lazy val androidId = AndroidId(defaultConfig.getString("ninecards.test.androidId"))
  lazy val localization = Localization(defaultConfig.getString("ninecards.test.localization"))

  lazy val marketAuth = MarketCredentials(androidId, token, Some(localization))
}

object TestConfig extends TestConfig
