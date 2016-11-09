package cards.nine.processes.utils

import cards.nine.commons.config.DummyConfig
import com.roundeights.hasher.Hasher
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class HashUtilsSpec
  extends Specification
  with DummyConfig
  with ScalaCheck {

  val hashUtils = new HashUtils

  "hashValue" should {
    "return a hMacSha512 hash value if no algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).hmac(ninecards.secretKey).sha512.hex

        hashUtils.hashValue(text = text) shouldEqual expectedHashValue
      }
    }

    "return a md5 hash value if md5 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).md5.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Md5
        ) shouldEqual expectedHashValue
      }
    }

    "return a sha256 hash value if sha256 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).sha256.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Sha256
        ) shouldEqual expectedHashValue
      }
    }

    "return a sha512 hash value if sha512 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).sha512.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Sha512
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacMd5 hash value if hMacMd5 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).hmac(ninecards.secretKey).md5.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacMd5
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacSha256 hash value if hMacSha256 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).hmac(ninecards.secretKey).sha256.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacSha256
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacSha512 hash value if hMacSha512 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(ninecards.salt).hmac(ninecards.secretKey).sha512.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacSha512
        ) shouldEqual expectedHashValue
      }
    }
  }
}
