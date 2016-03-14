package com.fortysevendeg.ninecards.api

import cats.free.Free
import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.api.utils.FreeUtils._
import com.fortysevendeg.ninecards.api.utils.TaskDirectives._
import com.fortysevendeg.ninecards.api.utils.TaskUtils._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes._
import shapeless._
import spray.routing.authentication._
import spray.routing.directives._
import spray.routing.{AuthenticationFailedRejection, Directive, Directive0}

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

class NineCardsAuthenticator(
  implicit userProcesses: UserProcesses[NineCardsServices],
  googleApiProcesses: GoogleApiProcesses[NineCardsServices],
  ec: ExecutionContext)
  extends HeaderDirectives
    with MarshallingDirectives
    with SecurityDirectives
    with JsonFormats {

  implicit def fromTaskAuth[T](
    auth: => Task[Authentication[T]]): AuthMagnet[T] =
    new AuthMagnet(onSuccess(auth))

  val rejectionByCredentialsRejected = AuthenticationFailedRejection(
    cause = AuthenticationFailedRejection.CredentialsRejected,
    challengeHeaders = Nil)

  def authenticateLoginRequest: Directive0 = {
    for {
      request <- entity(as[ApiLoginRequest])
      _ <- authenticate(validateLoginRequest(request.email, request.tokenId))
    } yield ()
  } flatMap { _ => Directive.Empty }

  def validateLoginRequest(
    email: String,
    tokenId: String): Task[Authentication[Boolean]] =
    (email, tokenId) match {
      case (e, o) if e.isEmpty || o.isEmpty =>
        Task.now(Left(rejectionByCredentialsRejected))
      case _ =>
        val task: Task[Boolean] = googleApiProcesses.checkGoogleTokenId(email, tokenId)

        task map {
          case value if value => Right(true)
          case _ => Left(rejectionByCredentialsRejected)
        } handle {
          case _ => Left(rejectionByCredentialsRejected)
        }
    }

  def authenticateUser: Directive[UserInfo] = for {
    sessionToken <- headerValueByName(headerSessionToken)
    androidId <- headerValueByName(headerAndroidId)
    userId <- authenticate(validateUser(sessionToken, androidId))
  } yield UserId(userId) :: AndroidId(androidId) :: HNil

  def validateUser(
    sessionToken: String,
    androidId: String): Task[Authentication[Long]] = {
    val task: Task[Option[Long]] = userProcesses.checkSessionToken(sessionToken, androidId)

    task map {
      case Some(v) => Right(v)
      case None =>
        Left(rejectionByCredentialsRejected)
    } handle {
      case _ => Left(rejectionByCredentialsRejected)
    }
  }

}

object NineCardsAuthenticator {

  implicit def nineCardsAuthenticator(
    implicit userProcesses: UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    ec: ExecutionContext) = new NineCardsAuthenticator

}
