package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.market.{ MarketToken, Localization }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  "toMarketAuth" should {
    "convert UserContext and GooglePlayContext objects to an AuthParams object" in {
      prop { (userId: UserId, androidId: AndroidId, token: MarketToken, localization: Option[Localization]) ⇒

        val userContext = UserContext(userId, androidId)
        val googlePlayContext = GooglePlayContext(token, localization)

        val marketAuth = Converters.toMarketAuth(googlePlayContext, userContext)

        marketAuth.token must_== token
        marketAuth.localization must_== localization
        marketAuth.androidId must_== androidId
      }
    }
  }

}
