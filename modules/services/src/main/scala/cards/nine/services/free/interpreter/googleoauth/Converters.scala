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

import cards.nine.domain.oauth.ServiceAccount
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import io.circe.Encoder
import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._

object Converters {

  import io.circe.generic.semiauto._
  import io.circe.syntax._

  // The GoogleCredential class has two small problems: the constructor for serviceAcount needs a Json text
  // passed through an Input Stream. Thus, we create a class to transform it to Json to pipe it through.
  private[this] case class CredentialsData(
    `type`: String,
    client_email: String,
    client_id: String,
    private_key: String,
    private_key_id: String,
    token_uri: String
  )

  private[this] implicit val dataEncoder: Encoder[CredentialsData] =
    deriveEncoder[CredentialsData]

  def toGoogleCredential(account: ServiceAccount): GoogleCredential = {

    import account._

    val data = CredentialsData(
      `type`         = "service_account",
      client_email   = clientEmail,
      client_id      = clientId,
      private_key    = privateKey,
      private_key_id = privateKeyId,
      token_uri      = tokenUri
    )

    val jsonStr: String = data.asJson.noSpaces
    GoogleCredential
      .fromStream(new ByteArrayInputStream(jsonStr.getBytes))
      .createScoped(scopes.asJava)

  }

}
