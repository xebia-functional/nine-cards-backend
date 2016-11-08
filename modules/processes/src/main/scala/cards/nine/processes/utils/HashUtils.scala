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