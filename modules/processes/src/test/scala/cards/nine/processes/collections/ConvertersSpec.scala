package cards.nine.processes.collections

import cards.nine.services.free.domain._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  "toGetSubscriptionsByUserResponse" should {
    "convert a list of SharedCollectionSubscription to a GetSubscriptionsByUserResponse object" in {
      prop { subscriptions: List[SharedCollectionSubscription] ⇒

        val response = Converters.toGetSubscriptionsByUserResponse(subscriptions)

        forall(response.subscriptions) { publicId ⇒
          subscriptions.exists(_.sharedCollectionPublicId == publicId)
        }
      }
    }
  }
}
