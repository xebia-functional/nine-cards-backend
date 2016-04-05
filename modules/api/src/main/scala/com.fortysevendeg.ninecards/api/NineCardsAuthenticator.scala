package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.api.utils.SprayMarshallers._
import com.fortysevendeg.ninecards.api.utils.TaskDirectives._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes._
import shapeless._
import spray.http.Uri
import spray.routing._
import spray.routing.authentication._
import spray.routing.directives._

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

class NineCardsAuthenticator(
  implicit
  userProcesses:      UserProcesses[NineCardsServices],
  googleApiProcesses: GoogleApiProcesses[NineCardsServices],
  ec:                 ExecutionContext
)
    extends HeaderDirectives
    with MarshallingDirectives
    with MiscDirectives
    with SecurityDirectives
    with JsonFormats {

  implicit def fromTaskAuth[T](
    auth: ⇒ Task[Authentication[T]]
  ): AuthMagnet[T] =
    new AuthMagnet(onSuccess(auth))

  val rejectionByCredentialsRejected = AuthenticationFailedRejection(
    cause            = AuthenticationFailedRejection.CredentialsRejected,
    challengeHeaders = Nil
  )

  def authenticateLoginRequest: Directive0 = {
    for {
      request ← entity(as[ApiLoginRequest])
      _ ← authenticate(validateLoginRequest(request.email, request.tokenId))
    } yield ()
  } flatMap { _ ⇒ Directive.Empty }

  def validateLoginRequest(
    email:   String,
    tokenId: String
  ): Task[Authentication[Unit]] =
    (email, tokenId) match {
      case (e, o) if e.isEmpty || o.isEmpty ⇒
        Task.now(Left(rejectionByCredentialsRejected))
      case _ ⇒
        googleApiProcesses.checkGoogleTokenId(email, tokenId).foldMap(interpreters) map {
          case true ⇒ Right(())
          case _    ⇒ Left(rejectionByCredentialsRejected)
        } handle {
          case _ ⇒ Left(rejectionByCredentialsRejected)
        }
    }

  def authenticateUser: Directive[UserInfo] = for {
    uri ← requestUri
    sessionToken ← headerValueByName(headerSessionToken)
    androidId ← headerValueByName(headerAndroidId)
    authToken ← headerValueByName(headerAuthToken)
    userId ← authenticate(validateUser(sessionToken, androidId, authToken, uri))
  } yield UserContext(UserId(userId), AndroidId(androidId)) :: HNil

  def validateUser(
    sessionToken: String,
    androidId:    String,
    authToken:    String,
    requestUri:   Uri
  ): Task[Authentication[Long]] =
    userProcesses.checkAuthToken(
      sessionToken = sessionToken,
      androidId    = androidId,
      authToken    = authToken,
      requestUri   = requestUri.toString
    ).foldMap(interpreters) map {
      case Some(v) ⇒ Right(v)
      case None ⇒
        Left(rejectionByCredentialsRejected)
    } handle {
      case _ ⇒ Left(rejectionByCredentialsRejected)
    }

}

object NineCardsAuthenticator {

  implicit def nineCardsAuthenticator(
    implicit
    userProcesses:      UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    ec:                 ExecutionContext
  ) = new NineCardsAuthenticator

}
