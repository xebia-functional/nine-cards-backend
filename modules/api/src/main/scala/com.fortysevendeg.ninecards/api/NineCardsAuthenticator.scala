package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.FreeUtils._
import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.processes.UserProcesses
import shapeless._
import spray.routing.authentication._
import spray.routing.directives.FutureDirectives._
import spray.routing.directives.HeaderDirectives._
import spray.routing.directives.{AuthMagnet, SecurityDirectives}
import spray.routing.{AuthenticationFailedRejection, Directive}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.concurrent.Task
import scalaz.{-\/, \/-}

class NineCardsAuthenticator(
  implicit userProcesses: UserProcesses[NineCardsServices],
  ec: ExecutionContext) extends SecurityDirectives {

  implicit def fromFutureAuth[T](
    auth: ⇒ Future[Authentication[T]]): AuthMagnet[T] =
    new AuthMagnet(onSuccess(auth))

  def authenticateUser: Directive[UserInfo] = for {
    sessionToken <- headerValueByName(headerSessionToken)
    androidId <- headerValueByName(headerAndroidId)
    userId <- authenticate(AuthMagnet.fromFutureAuth(validateUser(sessionToken, androidId)))
  } yield UserId(userId) :: AndroidId(androidId) :: HNil

  def validateUser(
    sessionToken: String,
    androidId: String): Future[Authentication[Long]] = {
    val result: Task[Option[Long]] = userProcesses.checkSessionToken(sessionToken, androidId)

    Future {
      result.attemptRun match {
        case -\/(e) =>
          Left(AuthenticationFailedRejection(
            cause = AuthenticationFailedRejection.CredentialsRejected,
            challengeHeaders = Nil))
        case \/-(value) => value match {
          case Some(v) => Right(v)
          case None =>
            Left(
              AuthenticationFailedRejection(
                cause = AuthenticationFailedRejection.CredentialsRejected,
                challengeHeaders = Nil))
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
