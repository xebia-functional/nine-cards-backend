package cards.nine.processes.converters

import cards.nine.domain.application.{ FullCardList }
import cards.nine.services.free.domain._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  "toLoginResponse" should {
    "convert an User to a LoginResponse object" in {
      prop { info: (User, Installation) ⇒

        val processLoginResponse = Converters.toLoginResponse(info)
        processLoginResponse.sessionToken shouldEqual info._1.sessionToken
      }
    }
  }

  "toUpdateInstallationResponse" should {
    "convert an Installation to a UpdateInstallationResponse object" in {
      prop { installation: Installation ⇒

        val processUpdateInstallationResponse = Converters.toUpdateInstallationResponse(installation)
        processUpdateInstallationResponse.androidId shouldEqual installation.androidId
        processUpdateInstallationResponse.deviceToken shouldEqual installation.deviceToken
      }
    }
  }

  "filterCategorized" should {
    "convert an AppsInfo to a GetAppsInfoResponse object" in {
      prop { appsInfo: FullCardList ⇒

        val appsWithoutCategories = appsInfo.cards.filter(app ⇒ app.categories.isEmpty)

        val categorizeAppsResponse = Converters.filterCategorized(appsInfo)

        categorizeAppsResponse.missing shouldEqual appsInfo.missing ++ appsWithoutCategories.map(_.packageName)

        forall(categorizeAppsResponse.cards) { item ⇒
          appsInfo.cards.exists { app ⇒
            app.packageName == item.packageName &&
              app.categories == item.categories &&
              app.categories.nonEmpty
          } should beTrue
        }
      }
    }
  }

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