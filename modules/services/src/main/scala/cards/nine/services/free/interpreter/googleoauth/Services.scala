/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.interpreter.googleoauth

import cards.nine.commons.NineCardsErrors.GoogleOAuthError
import cards.nine.commons.NineCardsService.Result
import cards.nine.domain.oauth._
import cards.nine.services.free.algebra.GoogleOAuth._
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.IOException
import scalaz.concurrent.Task

object Services extends Handler[Task] {

  override def fetchAcessToken(account: ServiceAccount): Task[Result[AccessToken]] =
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
