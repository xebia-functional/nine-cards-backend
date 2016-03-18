package com.fortysevendeg.ninecards.processes.utils

import com.roundeights.hasher.Hasher
import org.specs2.mutable.Specification

class HashUtilsSpec extends Specification with DummyNineCardsConfig {

  val hashUtils = new HashUtils
  val text = "Hash me!"
  val hasher = Hasher(text).salt(nineCardsSalt)
  val md5HashValue = hasher.md5.hex
  val sha256HashValue = hasher.sha256.hex
  val sha512HashValue = hasher.sha512.hex
  val hMacMd5HashValue = hasher.hmac(nineCardsSecretKey).md5.hex
  val hMacSha256HashValue = hasher.hmac(nineCardsSecretKey).sha256.hex
  val hMacSha512HashValue = hasher.hmac(nineCardsSecretKey).sha512.hex

  "hashValue" should {
    "return a hMacSha512 hash value if no algorithm is specified" in {
      hashUtils.hashValue(text = text) shouldEqual hMacSha512HashValue
    }
    "return a md5 hash value if md5 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.Md5) shouldEqual md5HashValue
    }
    "return a sha256 hash value if sha256 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.Sha256) shouldEqual sha256HashValue
    }
    "return a sha512 hash value if sha512 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.Sha512) shouldEqual sha512HashValue
    }
    "return a hMacMd5 hash value if hMacMd5 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.HMacMd5) shouldEqual hMacMd5HashValue
    }
    "return a hMacSha256 hash value if hMacSha256 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.HMacSha256) shouldEqual hMacSha256HashValue
    }
    "return a hMacSha512 hash value if hMacSha512 algorithm is specified" in {
      hashUtils.hashValue(
        text = text,
        algorithm = EncryptionAlgorithm.HMacSha512) shouldEqual hMacSha512HashValue
    }
  }
}
