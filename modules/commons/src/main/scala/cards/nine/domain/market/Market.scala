package cards.nine.domain.market

import cards.nine.domain.account.AndroidId

case class MarketToken(value: String) extends AnyVal

/**
  * A Market Localization contains a string that identifies the language
  * and the country for which the Market's information should be given.
  * Some examples are "es_ES", or "en_GB".
  */
case class Localization(value: String) extends AnyVal

case class MarketCredentials(
  androidId: AndroidId,
  token: MarketToken,
  localization: Option[Localization]
)
