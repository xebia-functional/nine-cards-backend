package com.fortysevendeg.ninecards.processes

import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices.GoogleApiServices
import com.fortysevendeg.ninecards.services.free.domain.{TokenInfo, WrongTokenInfo}
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait GoogleApiProcessesSpecification
  extends Specification
    with Matchers
    with Mockito
    with GoogleApiProcessesContext {

  trait BasicScope extends Scope {

    implicit val googleApiServices: GoogleApiServices[NineCardsServices] = mock[GoogleApiServices[NineCardsServices]]
    implicit val googleApiProcesses = new GoogleApiProcesses[NineCardsServices]

  }

  trait SuccessfulScope extends BasicScope {

    googleApiServices.getTokenInfo(tokenId) returns Free.pure(Xor.right(tokenInfo))

  }

  trait UnsuccessfulScope extends BasicScope {

    googleApiServices.getTokenInfo(tokenId) returns Free.pure(Xor.left(wrongTokenInfo))

  }

}

trait GoogleApiProcessesContext {

  val email = "valid.email@test.com"

  val wrongEmail = "wrong.email@test.com"

  val tokenId = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxMjI3MjFlZWQwYjQ1YmUxNWUzMGI2YThhOThjOTM3ZTJlNmQxN"

  val tokenInfo = TokenInfo(
    iss = "accounts.google.com",
    at_hash = "rXE2xjjK6YP9QtfKXTBZmQ",
    aud = "123456789012.apps.googleusercontent.com",
    sub = "106222693719864970737",
    email_verified = "true",
    azp = "123456789012.apps.googleusercontent.com",
    hd = Option("test.com"),
    email = email,
    iat = "1457529032",
    exp = "1457532632",
    alg = "RS256",
    kid = "7122721eed0b45be15e30b6a8a98c937e2e6d16d")

  val wrongTokenInfo = WrongTokenInfo(error_description = "Invalid Value")
}

class GoogleApiProcessesSpec
  extends GoogleApiProcessesSpecification
    with ScalaCheck {

  "checkGoogleTokenId" should {
    "return true if the given tokenId is valid" in new SuccessfulScope {
      val tokenIdValidation = googleApiProcesses.checkGoogleTokenId(email, tokenId)

      tokenIdValidation.foldMap(testInterpreters) should beTrue
    }

    "return false if the given tokenId is valid but the given email address is different" in new SuccessfulScope {
      val tokenIdValidation = googleApiProcesses.checkGoogleTokenId(wrongEmail, tokenId)

      tokenIdValidation.foldMap(testInterpreters) should beFalse
    }

    "return false if the given tokenId is not valid" in new UnsuccessfulScope {
      val tokenIdValidation = googleApiProcesses.checkGoogleTokenId(email, tokenId)

      tokenIdValidation.foldMap(testInterpreters) should beFalse
    }
  }
}
