package com.fortysevendeg.ninecards.api

import java.util.UUID

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.api.utils.TaskDirectives._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes._
import org.joda.time.DateTime
import shapeless._
import spray.http.Uri
import spray.routing._
import spray.routing.authentication._
import spray.routing.directives._

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

class NineCardsDirectives(
  implicit
  userProcesses: UserProcesses[NineCardsServices],
  googleApiProcesses: GoogleApiProcesses[NineCardsServices],
  ec: ExecutionContext
)
  extends BasicDirectives
  with HeaderDirectives
  with MarshallingDirectives
  with MiscDirectives
  with SecurityDirectives
  with JsonFormats {

  implicit def fromTaskAuth[T](auth: ⇒ Task[Authentication[T]]): AuthMagnet[T] =
    new AuthMagnet(onSuccess(auth))

  val rejectionByCredentialsRejected = AuthenticationFailedRejection(
    cause            = AuthenticationFailedRejection.CredentialsRejected,
    challengeHeaders = Nil
  )

  def authenticateLoginRequest: Directive1[SessionToken] = for {
    request ← entity(as[ApiLoginRequest])
    _ ← authenticate(validateLoginRequest(request.email, request.tokenId))
    sessionToken ← generateSessionToken
  } yield sessionToken

  def validateLoginRequest(email: String, tokenId: String): Task[Authentication[Unit]] =
    (email, tokenId) match {
      case (e, o) if e.isEmpty || o.isEmpty ⇒
        Task.now(Left(rejectionByCredentialsRejected))
      case _ ⇒
        googleApiProcesses.checkGoogleTokenId(email, tokenId).foldMap(prodInterpreters) map {
          case true ⇒ Right(())
          case _ ⇒ Left(rejectionByCredentialsRejected)
        } handle {
          case _ ⇒ Left(rejectionByCredentialsRejected)
        }
    }

  def authenticateUser: Directive1[UserContext] = for {
    uri ← requestUri
    sessionToken ← headerValueByName(headerSessionToken)
    androidId ← headerValueByName(headerAndroidId)
    authToken ← headerValueByName(headerAuthToken)
    userId ← authenticate(validateUser(sessionToken, androidId, authToken, uri))
  } yield UserContext(UserId(userId), AndroidId(androidId)) :: HNil

  def validateUser(
    sessionToken: String,
    androidId: String,
    authToken: String,
    requestUri: Uri
  ): Task[Authentication[Long]] =
    userProcesses.checkAuthToken(
      sessionToken = sessionToken,
      androidId    = androidId,
      authToken    = authToken,
      requestUri   = requestUri.toString
    ).foldMap(prodInterpreters) map {
      case Some(v) ⇒ Right(v)
      case None ⇒
        Left(rejectionByCredentialsRejected)
    } handle {
      case _ ⇒ Left(rejectionByCredentialsRejected)
    }

  def generateNewCollectionInfo: Directive1[NewSharedCollectionInfo] = for {
    currentDateTime ← provide(CurrentDateTime(DateTime.now))
    publicIdentifier ← provide(PublicIdentifier(UUID.randomUUID.toString))
  } yield NewSharedCollectionInfo(currentDateTime, publicIdentifier) :: HNil

  def generateSessionToken: Directive1[SessionToken] = provide(SessionToken(UUID.randomUUID.toString))
}

object NineCardsDirectives {

  implicit def nineCardsDirectives(
    implicit
    userProcesses: UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    ec: ExecutionContext
  ) = new NineCardsDirectives

}
