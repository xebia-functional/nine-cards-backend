package com.fortysevendeg.ninecards.services.common

import org.specs2.mutable.Specification

class NineCardsConfigSpec extends Specification {

  val intValue = 123
  val stringValue = "abc"

  val dummyConfig =
    s"""
       |nineCards {
       |  intValue = $intValue
       |  stringValue = $stringValue
       |}
     """.stripMargin

  val config = new NineCardsConfig(Option(dummyConfig))

  "getInt" should {
    "return an Int value if the key exists in the config file" in {
      config.getInt("nineCards.intValue") shouldEqual intValue
    }
  }

  "getString" should {
    "return a String value if the key exists in the config file" in {
      config.getString("nineCards.stringValue") shouldEqual stringValue
    }
  }

  "getOptionalInt" should {
    "return some Int value if the key exists in the config file" in {
      config.getOptionalInt("nineCards.intValue") should beSome[Int](intValue)
    }

    "return None if the key exists in the config file" in {
      config.getOptionalInt("intValue") should beNone
    }
  }

  "getOptionalString" should {
    "return some String value if the key exists in the config file" in {
      config.getOptionalString("nineCards.stringValue") should beSome[String](stringValue)
    }

    "return None if the key exists in the config file" in {
      config.getOptionalString("stringValue") should beNone
    }
  }
}
