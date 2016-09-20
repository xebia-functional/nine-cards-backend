package cards.nine.processes.utils

import com.roundeights.hasher.Hasher
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class HashUtilsSpec
  extends Specification
  with DummyNineCardsConfig
  with ScalaCheck {

  val hashUtils = new HashUtils

  "hashValue" should {
    "return a hMacSha512 hash value if no algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).hmac(nineCardsSecretKey).sha512.hex

        hashUtils.hashValue(text = text) shouldEqual expectedHashValue
      }
    }

    "return a md5 hash value if md5 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).md5.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Md5
        ) shouldEqual expectedHashValue
      }
    }

    "return a sha256 hash value if sha256 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).sha256.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Sha256
        ) shouldEqual expectedHashValue
      }
    }

    "return a sha512 hash value if sha512 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).sha512.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.Sha512
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacMd5 hash value if hMacMd5 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).hmac(nineCardsSecretKey).md5.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacMd5
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacSha256 hash value if hMacSha256 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).hmac(nineCardsSecretKey).sha256.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacSha256
        ) shouldEqual expectedHashValue
      }
    }

    "return a hMacSha512 hash value if hMacSha512 algorithm is specified" in {
      prop { (text: String) ⇒
        val expectedHashValue = Hasher(text).salt(nineCardsSalt).hmac(nineCardsSecretKey).sha512.hex

        hashUtils.hashValue(
          text      = text,
          algorithm = EncryptionAlgorithm.HMacSha512
        ) shouldEqual expectedHashValue
      }
    }
  }
}
