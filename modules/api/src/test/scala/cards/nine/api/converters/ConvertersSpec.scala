package cards.nine.api.converters

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.messages.InstallationsMessages._
import cards.nine.api.messages.UserMessages._
import cards.nine.domain.account.{ AndroidId, SessionToken }
import cards.nine.domain.market.{ MarketToken, Localization }
import cards.nine.processes.account.messages._
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
