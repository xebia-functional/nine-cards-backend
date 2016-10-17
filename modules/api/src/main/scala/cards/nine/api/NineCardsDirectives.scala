package cards.nine.api

import java.util.UUID

import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.NineCardsHeaders._
import cards.nine.api.messages.PathEnumerations.PriceFilter
import cards.nine.api.messages.UserMessages.ApiLoginRequest
import cards.nine.api.utils.SprayMatchers.PriceFilterSegment
import cards.nine.api.utils.TaskDirectives._
import cards.nine.domain.account._
import cards.nine.domain.market.{ MarketToken, Localization }
import cards.nine.processes.NineCardsServices._
import cards.nine.processes._
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
  with PathDirectives
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

  def validateLoginRequest(email: Email, tokenId: GoogleIdToken): Task[Authentication[Unit]] =
    (email, tokenId) match {
      case (e, o) if e.value.isEmpty || o.value.isEmpty ⇒
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
    herokuForwardedProtocol ← optionalHeaderValueByName(headerHerokuForwardedProto)
    herokuUri = herokuForwardedProtocol.filterNot(_.isEmpty).fold(uri)(uri.withScheme)
    sessionToken ← headerValueByName(headerSessionToken).map(SessionToken)
    androidId ← headerValueByName(headerAndroidId).map(AndroidId)
    authToken ← headerValueByName(headerAuthToken)
    userId ← authenticate(validateUser(sessionToken, androidId, authToken, herokuUri))
  } yield UserContext(UserId(userId), androidId) :: HNil

  val googlePlayInfo: Directive1[GooglePlayContext] = for {
    googlePlayToken ← headerValueByName(headerGooglePlayToken)
    marketLocalization ← optionalHeaderValueByName(headerMarketLocalization)
  } yield GooglePlayContext(
    googlePlayToken    = MarketToken(googlePlayToken),
    marketLocalization = marketLocalization map Localization.apply
  )

  def validateUser(
    sessionToken: SessionToken,
    androidId: AndroidId,
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

  val priceFilterPath: Directive[PriceFilter :: HNil] =
    path(PriceFilterSegment) | (pathEndOrSingleSlash & provide(PriceFilter.ALL: PriceFilter))
}

object NineCardsDirectives {

  implicit def nineCardsDirectives(
    implicit
    userProcesses: UserProcesses[NineCardsServices],
    googleApiProcesses: GoogleApiProcesses[NineCardsServices],
    ec: ExecutionContext
  ) = new NineCardsDirectives

}
