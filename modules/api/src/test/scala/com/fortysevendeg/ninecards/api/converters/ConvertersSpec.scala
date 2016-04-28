package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain.{ AndroidId, UserContext, UserId }
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._
import org.scalacheck.{Arbitrary, Gen}
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
      prop { (apiRequest: ApiLoginRequest, sessionToken: SessionToken) =>

        val request = Converters.toLoginRequest(apiRequest)(sessionToken)

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

        implicit val userContext = UserContext(userId, androidId)

        val request = Converters.toUpdateInstallationRequest(apiRequest)

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
}
