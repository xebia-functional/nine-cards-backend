package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.domain.account.{ AndroidId, SessionToken }
import cards.nine.domain.application.{ BasicCard, FullCardList }
import cards.nine.domain.market.{ MarketToken, Localization }
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

        request.androidId must_== apiRequest.androidId
        request.email must_== apiRequest.email
        request.sessionToken must_== sessionToken
        request.tokenId must_== apiRequest.tokenId
      }
    }
  }

  "toApiLoginResponse" should {
    "convert a LoginResponse to an ApiLoginResponse object" in {
      prop { response: LoginResponse ⇒

        val apiLoginResponse = Converters.toApiLoginResponse(response)

        apiLoginResponse.sessionToken must_== response.sessionToken
      }
    }
  }

  "toUpdateInstallationRequest" should {
    "convert an ApiUpdateInstallationRequest to a UpdateInstallationRequest object" in {
      prop { (apiRequest: ApiUpdateInstallationRequest, userId: UserId, androidId: AndroidId) ⇒

        val userContext = UserContext(userId, androidId)

        val request = Converters.toUpdateInstallationRequest(apiRequest, userContext)

        request.androidId must_== androidId
        request.deviceToken must_== apiRequest.deviceToken
        request.userId must_== userId.value
      }
    }
  }

  "toApiUpdateInstallationResponse" should {
    "convert an UpdateInstallationResponse to an ApiUpdateInstallationResponse object" in {
      prop { (response: UpdateInstallationResponse) ⇒

        val apiResponse = Converters.toApiUpdateInstallationResponse(response)

        apiResponse.androidId must_== response.androidId
        apiResponse.deviceToken must_== response.deviceToken
      }
    }
  }

  "toApiCategorizeAppsResponse" should {
    "convert an GetAppsInfoResponse to an ApiCategorizeAppsResponse object" in {
      prop { (response: FullCardList) ⇒

        val apiResponse = Converters.toApiAppsInfoResponse(Converters.toApiCategorizedApp)(response)

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
      prop { (response: FullCardList) ⇒

        val apiResponse = Converters.toApiAppsInfoResponse(Converters.toApiDetailsApp)(response)

        apiResponse.errors must_== response.missing
        apiResponse.items must_== (response.cards map Converters.toApiDetailsApp)
      }
    }
  }

  "toApiIconApp" should {
    "convert a FullCard to an ApiAppIcon" in
      prop { (card: BasicCard) ⇒
        val api = Converters.toApiIconApp(card)
        api.packageName must_== card.packageName
        api.title must_== card.title
        api.icon must_== card.icon
      }
  }

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
}
