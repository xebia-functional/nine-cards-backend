package cards.nine.api.collections

import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol._
import spray.json._

class JsonFormatsSpec extends Specification with Matchers {

  sequential

  "JodaDateTimeFormat, the Json Format for dates," should {

    import cards.nine.api.collections.JsonFormats.JodaDateTimeFormat

    val date = new DateTime(2013, 5, 23, 0, 0, DateTimeZone.UTC)

    "transfer it to a Json string as format" in {
      date.toJson must beEqualTo(JsString("2013-05-23T00:00:00.000+0000"))
    }

    "parse it from a Json String" in {
      val str = """ "2013-05-23T00:00:00.000+0000" """
      str.parseJson.convertTo[DateTime] shouldEqual date
    }

    case class MyDate(date: DateTime)
    implicit val format = jsonFormat1(MyDate)

    "use it properly in a record" in {
      val str = """ { "date" : "2013-05-23T00:00:00.000Z" } """
      str.parseJson.convertTo[MyDate] shouldEqual MyDate(date)
    }

  }

}
