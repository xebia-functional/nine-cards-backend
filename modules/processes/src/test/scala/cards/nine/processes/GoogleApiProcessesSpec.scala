package cards.nine.processes

import cards.nine.commons.NineCardsErrors.WrongGoogleAuthToken
import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.account._
import cards.nine.processes.NineCardsServices._
import cards.nine.services.free.algebra.GoogleApi.Services
import cards.nine.services.free.domain.TokenInfo
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait GoogleApiProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with TestInterpreters {

  val email = Email("valid.email@test.com")

  val wrongEmail = Email("wrong.email@test.com")

  val tokenId = GoogleIdToken("eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxMjI3MjFlZWQwYjQ1YmUxNWUzMGI2YThhOThjOTM3ZTJlNmQxN")

  val tokenInfo = TokenInfo("true", email.value)

  val wrongAuthTokenError = WrongGoogleAuthToken(message = "Invalid Value")

  trait BasicScope extends Scope {

    implicit val googleApiServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]

    val googleApiProcesses = new GoogleApiProcesses[NineCardsServices]

  }

}

class GoogleApiProcessesSpec
  extends GoogleApiProcessesSpecification
  with ScalaCheck {

  "checkGoogleTokenId" should {
    "return true if the given tokenId is valid" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.right(tokenInfo)

      googleApiProcesses
        .checkGoogleTokenId(email, tokenId)
        .foldMap(testInterpreters) must beRight(true)
    }

    "return false if the given tokenId is valid but the given email address is different" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.right(tokenInfo)

      googleApiProcesses
        .checkGoogleTokenId(wrongEmail, tokenId)
        .foldMap(testInterpreters) must beRight(false)
    }

    "return a WrongGoogleAuthToken error if the given tokenId is not valid" in new BasicScope {

      googleApiServices.getTokenInfo(GoogleIdToken(any[String])) returns NineCardsService.left(wrongAuthTokenError)

      googleApiProcesses
        .checkGoogleTokenId(email, tokenId)
        .foldMap(testInterpreters) must beLeft(wrongAuthTokenError)
    }
  }
}
