package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.api.utils.FreeUtils._
import com.fortysevendeg.ninecards.api.utils.TaskUtils._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.UserProcesses
import shapeless._
import spray.routing.authentication._
import spray.routing.directives.FutureDirectives._
import spray.routing.directives._
import spray.routing.{AuthenticationFailedRejection, Directive, Directive0}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.concurrent.Task
import scalaz.{-\/, \/-}

class NineCardsAuthenticator(
  implicit userProcesses: UserProcesses[NineCardsServices],
  ec: ExecutionContext)
  extends HeaderDirectives
    with MarshallingDirectives
    with SecurityDirectives
    with JsonFormats {

  implicit def fromFutureAuth[T](
    auth: => Future[Authentication[T]]): AuthMagnet[T] =
    new AuthMagnet(onSuccess(auth))

  val rejectionByCredentialsRejected = AuthenticationFailedRejection(
    cause = AuthenticationFailedRejection.CredentialsRejected,
    challengeHeaders = Nil)

  def authenticateLoginRequest: Directive0 = {
    for {
      request <- entity(as[ApiLoginRequest])
      _ <- authenticate(validateLoginRequest(request.email, request.oauthToken))
    } yield ()
  } flatMap { _ => Directive.Empty }

  /* TODO: We are only checking if the provided email and oauth token are empty. We should
   * research how to validate the Google Oauth token. See more in:
   * https://developers.google.com/identity/protocols/OAuth2UserAgent#validatetoken */

  def validateLoginRequest(
    email: String,
    oauthToken: String): Future[Authentication[Boolean]] =
    Future {
      (email, oauthToken) match {
        case (e, o) if e.isEmpty || o.isEmpty =>
          Left(rejectionByCredentialsRejected)
        case _ => Right(true)
      }
    }

  def authenticateUser: Directive[UserInfo] = for {
    sessionToken <- headerValueByName(headerSessionToken)
    androidId <- headerValueByName(headerAndroidId)
    userId <- authenticate(validateUser(sessionToken, androidId))
  } yield UserId(userId) :: AndroidId(androidId) :: HNil

  def validateUser(
    sessionToken: String,
    androidId: String): Future[Authentication[Long]] = {
    val result: Task[Option[Long]] = userProcesses.checkSessionToken(sessionToken, androidId)

    Future {
      result.attemptRun match {
        case -\/(e) =>
          Left(rejectionByCredentialsRejected)
        case \/-(value) => value match {
          case Some(v) => Right(v)
          case None =>
            Left(rejectionByCredentialsRejected)
        }
      }
    }
  }

}

object NineCardsAuthenticator {

  implicit def nineCardsAuthenticator(
    implicit userProcesses: UserProcesses[NineCardsServices],
    ec: ExecutionContext) = new NineCardsAuthenticator

}
