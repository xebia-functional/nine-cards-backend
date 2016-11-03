package cards.nine.commons

import cards.nine.commons.config.NineCardsConfig
import org.specs2.mutable.Specification

class NineCardsConfigSpec extends Specification {

  val intValue = 123
  val stringValue = "abc"
  val stringValue2 = "xyz"
  val booleanValue = true

  val dummyConfigHocon =
    s"""
       |nineCards {
       |  intValue = $intValue
       |  stringValue = $stringValue
       |  booleanValue = $booleanValue
       |  emptyStringListValue = []
       |  singletonStringListValue = [$stringValue]
       |  manyStringListValue = [$stringValue, $stringValue2 ]
       |}
     """.stripMargin

  val config = new NineCardsConfig(Option(dummyConfigHocon))

  private[this] val ifExists = "if the key exists in the config file"
  private[this] val ifNotExists = "if the key doesn't exist in the config file"

  "getBoolean" should {
    s"return an Boolean value $ifExists" in {
      config.getBoolean("nineCards.booleanValue") must_== booleanValue
    }
  }

  "getInt" should {
    s"return an Int value $ifExists" in {
      config.getInt("nineCards.intValue") must_== intValue
    }
  }

  "getString" should {
    s"return a String value $ifExists" in {
      config.getString("nineCards.stringValue") must_== stringValue
    }
  }

  "getStringList" should {

    s"return an empty list of String values $ifExists and the value is an empty list" in {
      config.getStringList("nineCards.emptyStringListValue") must_== List()
    }

    s"return a non-empty list of String values $ifExists and the value is not empty" in {
      config.getStringList("nineCards.singletonStringListValue") must_== List(stringValue)
    }

    s"return a list of several values $ifExists and the value is a comma-separated list" in {
      config.getStringList("nineCards.manyStringListValue") must_== List(stringValue, stringValue2)
    }

  }

  "getBoolean" should {
    s"return some Boolean value $ifExists" in {
      config.getOptionalBoolean("nineCards.booleanValue") must beSome[Boolean](booleanValue)
    }

    s"return None $ifNotExists" in {
      config.getOptionalBoolean("booleanValue") must beNone
    }
  }

  "getOptionalInt" should {
    s"return some Int value $ifExists" in {
      config.getOptionalInt("nineCards.intValue") must beSome[Int](intValue)
    }

    s"return None $ifNotExists " in {
      config.getOptionalInt("intValue") must beNone
    }
  }

  "getOptionalString" should {
    s"return some String value $ifExists" in {
      config.getOptionalString("nineCards.stringValue") must beSome[String](stringValue)
    }

    s"return None $ifNotExists" in {
      config.getOptionalString("stringValue") must beNone
    }
  }
}
