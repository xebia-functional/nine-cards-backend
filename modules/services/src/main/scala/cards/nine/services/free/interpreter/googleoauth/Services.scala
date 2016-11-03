package cards.nine.services.free.interpreter.googleoauth

import cards.nine.commons.NineCardsErrors.GoogleOAuthError
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.oauth._
import cards.nine.services.free.algebra.GoogleOAuth._
import cats.~>
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.IOException
import scalaz.concurrent.Task

object Services extends (Ops ~> Task) {

  override def apply[A](ops: Ops[A]): Task[A] = ops match {
    case FetchAccessToken(account: ServiceAccount) ⇒
      fetchAcessToken(account)
  }

  private[this] def fetchAcessToken(account: ServiceAccount): Task[Result[AccessToken]] =
    Task {
      val credential: GoogleCredential = Converters.toGoogleCredential(account)
      // A GoogleCredential is stateful object that can contain token.
      // refreshToken is command to fetch token from server. Boolean response indicates success
      val success = credential.refreshToken()
      if (success) {
        Right(AccessToken(credential.getAccessToken()))
      } else {
        import account._
        val message = s"Failed to obtain an OAuth Access Token for the service account $clientEmail to the scopes $scopes"
        Left(GoogleOAuthError(message))
      }
    }.handle {
      case e: IOException ⇒ Left(GoogleOAuthError(e.getMessage()))
    }

}
