package com.fortysevendeg.ninecards.services.interpreter.impl

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.{TokenInfo, WrongTokenInfo}
import com.fortysevendeg.ninecards.services.free.interpreter.impl._
import com.fortysevendeg.ninecards.services.utils.{MockServerService, XorMatchers}
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._
import org.mockserver.model.HttpStatusCode
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification

trait GoogleApiServerResponse {

  val getTokenInfoValidResponse =
    """
      |{
      | "iss": "accounts.google.com",
      | "at_hash": "miKQC8jFj8FFAxDoK4HTSA",
      | "aud": "407408718192.apps.googleusercontent.com",
      | "sub": "106222693719864970737",
      | "email_verified": "true",
      | "azp": "407408718192.apps.googleusercontent.com",
      | "hd": "example.com",
      | "email": "user@example.com",
      | "iat": "1457605848",
      | "exp": "1457609448",
      | "alg": "RS256",
      | "kid": "eece476bc7e07fb1efec961e1ab277ebace0fe0f"
      |}
    """.stripMargin

  val getTokenInfoValidResponseWithoutHd =
    """
      |{
      | "iss": "accounts.google.com",
      | "at_hash": "miKQC8jFj8FFAxDoK4HTSA",
      | "aud": "407408718192.apps.googleusercontent.com",
      | "sub": "106222693719864970737",
      | "email_verified": "true",
      | "azp": "407408718192.apps.googleusercontent.com",
      | "email": "user@gmailcom",
      | "iat": "1457605848",
      | "exp": "1457609448",
      | "alg": "RS256",
      | "kid": "eece476bc7e07fb1efec961e1ab277ebace0fe0f"
      |}
    """.stripMargin

  val getTokenInfoWrongResponse =
    """
      |{
      | "error_description": "Invalid Value"
      |}
    """.stripMargin

}

trait MockGoogleApiServer
  extends MockServerService
    with GoogleApiServerResponse {

  val getTokenInfoPath = "/oauth2/v3/tokeninfo"
  val tokenIdParameterName = "id_token"

  val validTokenId = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImVlY2U0NzZiYzdlMDdmYjFlZmVjOTYxZTFhYjI3N2ViYWN"
  val otherTokenId = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjZkNjQzY2Y5MGI1NTgyOTg0YjRlZTY3MjI4NGMzMzI0ZTg"
  val wrongTokenId = "eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiYXRfaGFzaCI6Im1pS1FDOGpGajhGRkF4RG9"
  val failingTokenId = "1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDYyMjI2OTM3MTk4NjQ5NzA3MzciLCJlbWFpbF92ZX"

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(getTokenInfoPath)
      .withQueryStringParameter(tokenIdParameterName, validTokenId))
    .respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(getTokenInfoValidResponse))

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(getTokenInfoPath)
      .withQueryStringParameter(tokenIdParameterName, otherTokenId))
    .respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(getTokenInfoValidResponseWithoutHd))

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(getTokenInfoPath)
      .withQueryStringParameter(tokenIdParameterName, wrongTokenId))
    .respond(
      response
        .withStatusCode(HttpStatusCode.OK_200.code)
        .withHeader(jsonHeader)
        .withBody(getTokenInfoWrongResponse))

  mockServer.when(
    request
      .withMethod("GET")
      .withPath(getTokenInfoPath)
      .withQueryStringParameter(tokenIdParameterName, failingTokenId))
    .respond(
      response
        .withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code))
}

class GoogleApiServicesSpec
  extends Specification
    with DisjunctionMatchers
    with XorMatchers
    with MockGoogleApiServer {

  implicit val googleApiConfiguration = GoogleApiConfiguration(
    protocol = "http",
    host = "localhost",
    port = Option(mockServerPort),
    tokenInfoUri = getTokenInfoPath,
    tokenIdQueryParameter = tokenIdParameterName)

  val googleApiServices = GoogleApiServices.googleApiServices

  "getTokenInfo" should {
    "return the TokenInfo object when a valid token id is provided" in {
      val response = googleApiServices.getTokenInfo(validTokenId)

      response.attemptRun should be_\/-[WrongTokenInfo Xor TokenInfo].which {
        content =>
          content should beXorRight[TokenInfo]
      }
    }
    "return the TokenInfo object when a valid token id is provided and the hd field isn't" +
      "included into the response" in {
      val response = googleApiServices.getTokenInfo(otherTokenId)

      response.attemptRun should be_\/-[WrongTokenInfo Xor TokenInfo].which {
        content =>
          content should beXorRight[TokenInfo]
      }
    }
    "return a WrongTokenInfo object when a wrong token id is provided" in {
      val result = googleApiServices.getTokenInfo(wrongTokenId)

      result.attemptRun should be_\/-[WrongTokenInfo Xor TokenInfo].which {
        content =>
          content should beXorLeft[WrongTokenInfo]
      }
    }
    "return an exception when something fails during the call to the Google API" in {
      val result = googleApiServices.getTokenInfo(failingTokenId)

      result.attemptRun should be_-\/[Throwable]
    }
  }

}
