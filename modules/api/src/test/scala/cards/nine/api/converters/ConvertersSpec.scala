package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.market.{ MarketToken, Localization }
import cards.nine.processes.messages.ApplicationMessages._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.messages.UserMessages._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  val uuidGenerator: Gen[String] = Gen.uuid.map(_.toString)
  implicit val abSessionToken: Arbitrary[SessionToken] = Arbitrary(uuidGenerator.map(SessionToken.apply))

  "toLoginRequest" should {
    "convert an ApiLoginRequest to a LoginRequest object" in {
      prop { (apiRequest: ApiLoginRequest, sessionToken: SessionToken) ⇒

        val request = Converters.toLoginRequest(apiRequest, sessionToken)

        request.androidId shouldEqual apiRequest.androidId
        request.email shouldEqual apiRequest.email
        request.sessionToken shouldEqual sessionToken.value
        request.tokenId shouldEqual apiRequest.tokenId
      }
    }
  }

  "toApiLoginResponse" should {
    "convert a LoginResponse to an ApiLoginResponse object" in {
      prop { response: LoginResponse ⇒

        val apiLoginResponse = Converters.toApiLoginResponse(response)

        apiLoginResponse.sessionToken shouldEqual response.sessionToken
      }
    }
  }

  "toUpdateInstallationRequest" should {
    "convert an ApiUpdateInstallationRequest to a UpdateInstallationRequest object" in {
      prop { (apiRequest: ApiUpdateInstallationRequest, userId: UserId, androidId: AndroidId) ⇒

        val userContext = UserContext(userId, androidId)

        val request = Converters.toUpdateInstallationRequest(apiRequest, userContext)

        request.androidId shouldEqual androidId.value
        request.deviceToken shouldEqual apiRequest.deviceToken
        request.userId shouldEqual userId.value
      }
    }
  }

  "toApiUpdateInstallationResponse" should {
    "convert an UpdateInstallationResponse to an ApiUpdateInstallationResponse object" in {
      prop { (response: UpdateInstallationResponse) ⇒

        val apiResponse = Converters.toApiUpdateInstallationResponse(response)

        apiResponse.androidId shouldEqual response.androidId
        apiResponse.deviceToken shouldEqual response.deviceToken
      }
    }
  }

  "toApiCategorizeAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiCategorizeAppsResponse object" in {
      prop { (response: GetAppsInfoResponse) ⇒

        val apiResponse = Converters.toApiCategorizeAppsResponse(response)

        apiResponse.errors shouldEqual response.errors
        forall(apiResponse.items) { item ⇒
          response.items.exists(appInfo ⇒
            appInfo.packageName == item.packageName &&
              (
                (appInfo.categories.nonEmpty && appInfo.categories.contains(item.category)) ||
                (appInfo.categories.isEmpty && item.category.isEmpty)
              ))
        }
      }
    }
  }

  "toApiDetailAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiDetailAppsResponse object" in {
      prop { (response: GetAppsInfoResponse) ⇒

        val apiResponse = Converters.toApiDetailAppsResponse(response)

        apiResponse.errors shouldEqual response.errors
        apiResponse.items shouldEqual response.items
      }
    }
  }

  "toMarketAuth" should {
    "convert UserContext and GooglePlayContext objects to an AuthParams object" in {
      prop { (userId: UserId, androidId: AndroidId, token: MarketToken, localization: Option[Localization]) ⇒

        val userContext = UserContext(userId, androidId)
        val googlePlayContext = GooglePlayContext(token, localization)

        val marketAuth = Converters.toMarketAuth(googlePlayContext, userContext)

        marketAuth.token shouldEqual token
        marketAuth.localization shouldEqual localization
        marketAuth.androidId shouldEqual androidId
      }
    }
  }

  "toApiCreateOrUpdateCollectionResponse" should {
    "convert CreateOrUpdateCollectionResponse to ApiCreateOrUpdateCollectionResponse" in {
      prop { (response: CreateOrUpdateCollectionResponse) ⇒

        val apiResponse = Converters.toApiCreateOrUpdateCollectionResponse(response)

        apiResponse.packagesStats shouldEqual response.packagesStats
        apiResponse.publicIdentifier shouldEqual response.publicIdentifier
      }
    }
  }

  "toApiGetSubscriptionsByUserResponse" should {
    "convert GetSubscriptionsByUserResponse to ApiGetSubscriptionsByUserResponse" in {
      prop { (response: GetSubscriptionsByUserResponse) ⇒

        val apiResponse = Converters.toApiGetSubscriptionsByUser(response)

        apiResponse.subscriptions shouldEqual response.subscriptions
      }
    }
  }
}
