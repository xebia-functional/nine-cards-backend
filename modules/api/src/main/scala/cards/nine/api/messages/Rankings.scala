package cards.nine.api.messages

import cards.nine.domain.application.Package

import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat

object rankings {

  case class Ranking(categories: Map[String, List[Package]])

  object Reload {

    case class Request(
      startDate: DateTime,
      endDate: DateTime,
      rankingLength: Int
    ) {
      if (!isValidDateRange(startDate, endDate))
        throw new InvalidDate(startDate, endDate)
    }

    val formatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC

    class InvalidDate(startDate: DateTime, endDate: DateTime) extends Throwable {
      override def getMessage(): String = {
        s"""Invalid date range { startDate=${formatter.print(startDate)}, endDate = ${formatter.print(endDate)} }
          | For a date range to be valid,
          | * The startDate and endDate must be formatted as "YYYY-MM-DD", such as "2016-04-15".
          | * The startDate must be before the endDate
          | * The startDate must be after Google Analytics launch date, in 2015-01-01"
        """.stripMargin
      }
    }

    private[this] val googleAnalyticsLaunch: DateTime =
      new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC)

    def isValidDateRange(startDate: DateTime, endDate: DateTime): Boolean = {
      (startDate isBefore endDate) && (startDate isAfter googleAnalyticsLaunch)
    }

    case class Response()

    case class Error(code: Int, message: String, status: String) extends Throwable

  }

  object Get {
    case class Response(ranking: Ranking)
  }

}