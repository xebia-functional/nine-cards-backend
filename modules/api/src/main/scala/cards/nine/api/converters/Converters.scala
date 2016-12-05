package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.domain.market.MarketCredentials

object Converters {

  def toMarketAuth(googlePlayContext: GooglePlayContext, userContext: UserContext): MarketCredentials =
    MarketCredentials(
      androidId    = userContext.androidId,
      localization = googlePlayContext.marketLocalization,
      token        = googlePlayContext.googlePlayToken
    )

}
