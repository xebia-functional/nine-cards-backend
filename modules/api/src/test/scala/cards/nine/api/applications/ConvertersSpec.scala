package cards.nine.api.applications

import cards.nine.domain.application.{ BasicCard, CardList, FullCard }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  import Converters._

  "toApiCategorizeAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiCategorizeAppsResponse object" in {
      prop { (response: CardList[FullCard]) ⇒

        val apiResponse = toApiAppsInfoResponse(toApiCategorizedApp)(response)

        apiResponse.errors must containTheSameElementsAs(response.missing)

        forall(apiResponse.items) { item ⇒
          response.cards.exists(appInfo ⇒
            appInfo.packageName == item.packageName &&
              appInfo.categories == item.categories)
        }
      }
    }
  }

  "toApiDetailAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiDetailAppsResponse object" in {
      prop { (response: CardList[FullCard]) ⇒

        val apiResponse = toApiAppsInfoResponse(toApiDetailsApp)(response)

        apiResponse.errors must_== response.missing
        apiResponse.items must_== (response.cards map toApiDetailsApp)
      }
    }
  }

  "toApiIconApp" should {
    "convert a FullCard to an ApiAppIcon" in
      prop { (card: BasicCard) ⇒
        val api = toApiIconApp(card)
        api.packageName must_== card.packageName
        api.title must_== card.title
        api.icon must_== card.icon
      }
  }

}
