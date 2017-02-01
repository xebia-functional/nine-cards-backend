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

import cards.nine.commons.config.DummyConfig
import cards.nine.domain.oauth.ServiceAccount
import java.util.{ Iterator ⇒ JIter }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConverterSpec
  extends Specification
  with DummyConfig
  with ScalaCheck {

  import cards.nine.domain.ScalaCheck.arbServiceAccount
  import scala.collection.JavaConverters._

  "toGoogleCredential" should {
    "obtain valid credentials for a service account" in
      prop { account: ServiceAccount ⇒
        val cred = Converters.toGoogleCredential(account)
        cred.getServiceAccountId() shouldEqual account.clientEmail
        cred.getServiceAccountPrivateKeyId() shouldEqual account.privateKeyId
        val scopes = cred.getServiceAccountScopes.iterator().asInstanceOf[JIter[String]].asScala.toList
        scopes should containTheSameElementsAs(account.scopes)
      }
  }

}
