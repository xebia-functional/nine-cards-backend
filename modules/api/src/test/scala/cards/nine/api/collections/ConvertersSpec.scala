package cards.nine.api.collections

import cards.nine.processes.collections.messages._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  "toApiCreateOrUpdateCollectionResponse" should {
    "convert CreateOrUpdateCollectionResponse to ApiCreateOrUpdateCollectionResponse" in {
      prop { (response: CreateOrUpdateCollectionResponse) ⇒

        val apiResponse = Converters.toApiCreateOrUpdateCollectionResponse(response)

        apiResponse.packagesStats must_== response.packagesStats
        apiResponse.publicIdentifier must_== response.publicIdentifier
      }
    }
  }

  "toApiGetSubscriptionsByUserResponse" should {
    "convert GetSubscriptionsByUserResponse to ApiGetSubscriptionsByUserResponse" in {
      prop { (response: GetSubscriptionsByUserResponse) ⇒

        val apiResponse = Converters.toApiGetSubscriptionsByUser(response)

        apiResponse.subscriptions must_== response.subscriptions
      }
    }
  }

  "toApiIncreaseViewsCountByOneResponse" should {
    "convert IncreaseViewsCountByOneResponse to ApiIncreaseViewsCountByOneResponse" in {
      prop { (response: IncreaseViewsCountByOneResponse) ⇒

        val apiResponse = Converters.toApiIncreaseViewsCountByOneResponse(response)

        apiResponse.publicIdentifier must_== response.publicIdentifier
      }
    }
  }
}
