package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.services.free.domain.Category
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import spray.json._

class JsonFormatsSpecification extends Specification with Matchers {

  sequential

  import com.fortysevendeg.ninecards.api.JsonFormats._

  "Formatting of a Category" should {
    import com.fortysevendeg.ninecards.api.JsonFormats.category
    val social: Category = Category.SOCIAL

    "transform it to a Json string with the name of the category" in {
      social.toJson shouldEqual JsString("SOCIAL")
    }

    "convert it from Json string" in {
      val json = JsString("SOCIAL")
      json.convertTo[Category] shouldEqual (Category.SOCIAL)
    }.pendingUntilFixed()

  }

  "Formatting of a Date" should {

    implicit val JodaDateTimeFormat: RootJsonFormat[DateTime] =
      jodaDateTimeFormat(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").withZoneUTC)

    val date = new DateTime(2013, 5, 23, 0, 0, DateTimeZone.UTC)

    "transfer it to a Json string as format" in {
      date.toJson must beEqualTo(JsString("2013-05-23T00:00:00.000000"))
    }

    "parse it from a Json String" in {
      val str = """ "2013-05-23T00:00:00.000000" """
      str.parseJson.convertTo[DateTime] shouldEqual date
    }.pendingUntilFixed()

    case class MyDate(date: DateTime)
    implicit val format = jsonFormat1(MyDate)

    "use it properly in a record" in {
      val str = """ { "date" : "2013-05-23T00:00:00.000000" } """
      str.parseJson.convertTo[MyDate] shouldEqual MyDate(date)
    }.pendingUntilFixed()

  }

}