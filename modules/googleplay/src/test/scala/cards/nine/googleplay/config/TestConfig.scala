package cards.nine.googleplay.config

import cards.nine.commons.config.NineCardsConfig._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }

trait TestConfig {

  lazy val token = MarketToken(nineCardsConfiguration.test.token)
  lazy val androidId = AndroidId(nineCardsConfiguration.test.androidId)
  lazy val localization = Localization(nineCardsConfiguration.test.localization)

  lazy val marketAuth = MarketCredentials(androidId, token, Some(localization))
}

object TestConfig extends TestConfig
