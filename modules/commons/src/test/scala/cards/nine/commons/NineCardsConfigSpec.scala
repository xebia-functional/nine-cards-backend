package cards.nine.commons

import org.specs2.mutable.Specification

class NineCardsConfigSpec extends Specification {

  val intValue = 123
  val stringValue = "abc"
  val booleanValue = true

  val dummyConfigHocon =
    s"""
       |nineCards {
       |  intValue = $intValue
       |  stringValue = $stringValue
       |  booleanValue = $booleanValue
       |}
     """.stripMargin

  val config = new NineCardsConfig(Option(dummyConfigHocon))

  "getBoolean" should {
    "return an Boolean value if the key exists in the config file" in {
      config.getBoolean("nineCards.booleanValue") must_== booleanValue
    }
  }

  "getInt" should {
    "return an Int value if the key exists in the config file" in {
      config.getInt("nineCards.intValue") must_== intValue
    }
  }

  "getString" should {
    "return a String value if the key exists in the config file" in {
      config.getString("nineCards.stringValue") must_== stringValue
    }
  }

  "getBoolean" should {
    "return some Boolean value if the key exists in the config file" in {
      config.getOptionalBoolean("nineCards.booleanValue") must beSome[Boolean](booleanValue)
    }

    "return None if the key doesn't exist in the config file" in {
      config.getOptionalBoolean("booleanValue") must beNone
    }
  }

  "getOptionalInt" should {
    "return some Int value if the key exists in the config file" in {
      config.getOptionalInt("nineCards.intValue") must beSome[Int](intValue)
    }

    "return None if the key doesn't exist in the config file" in {
      config.getOptionalInt("intValue") must beNone
    }
  }

  "getOptionalString" should {
    "return some String value if the key exists in the config file" in {
      config.getOptionalString("nineCards.stringValue") must beSome[String](stringValue)
    }

    "return None if the key doesn't exist in the config file" in {
      config.getOptionalString("stringValue") must beNone
    }
  }
}
