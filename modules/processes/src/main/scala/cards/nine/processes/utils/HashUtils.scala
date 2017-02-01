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
package cards.nine.processes.utils

import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.processes.utils.EncryptionAlgorithm._
import com.roundeights.hasher.Hasher

object EncryptionAlgorithm {

  sealed trait Algorithm

  case object Md5 extends Algorithm

  case object Sha256 extends Algorithm

  case object Sha512 extends Algorithm

  case object HMacMd5 extends Algorithm

  case object HMacSha256 extends Algorithm

  case object HMacSha512 extends Algorithm

}

class HashUtils(implicit config: NineCardsConfiguration) {

  lazy val nineCardsSalt: Option[String] = config.salt
  lazy val nineCardsSecretKey: String = config.secretKey

  def hashValue(
    text: String,
    secretKey: String = nineCardsSecretKey,
    salt: Option[String] = nineCardsSalt,
    algorithm: Algorithm = HMacSha512
  ) = {

    val hasher = salt.fold(Hasher(text))(Hasher(text).salt(_))

    val digest = algorithm match {
      case Md5 ⇒ hasher.md5
      case Sha256 ⇒ hasher.sha256
      case Sha512 ⇒ hasher.sha512
      case HMacMd5 ⇒ hasher.hmac(secretKey).md5
      case HMacSha256 ⇒ hasher.hmac(secretKey).sha256
      case HMacSha512 ⇒ hasher.hmac(secretKey).sha512
    }

    digest.hex
  }
}

object HashUtils {

  implicit def hashUtils(implicit config: NineCardsConfiguration) = new HashUtils
}