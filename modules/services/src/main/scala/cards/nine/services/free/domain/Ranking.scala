package cards.nine.services.free.domain

import cats.data.Xor
import org.joda.time.DateTime

object rankings {

  import enumeratum._

  case class AuthParams(access_token: String) extends AnyVal

  /* A date range specifies a contiguous set of days: startDate, startDate + 1 day, ..., endDate.
   https://developers.google.com/analytics/devguides/reporting/core/v4/rest/v4/reports/batchGet#DateRange */
  case class DateRange(startDate: DateTime, endDate: DateTime)

  case class RankingParams(
    dateRange: DateRange,
    rankingLength: Int,
    auth: AuthParams
  )

  sealed trait Continent extends EnumEntry
  object Continent extends Enum[Continent] {
    case object Americas extends Continent
    case object Africa extends Continent
    case object Asia extends Continent
    case object Europe extends Continent
    case object Oceania extends Continent

    val values = super.findValues
  }

  sealed trait Country extends EnumEntry
  object Country extends Enum[Country] {
    case object Spain extends Country
    case object United_Kingdom extends Country
    case object United_States extends Country

    val values = super.findValues
  }

  sealed trait GeoScope
  case class CountryScope(country: Country) extends GeoScope
  case class ContinentScope(continent: Continent) extends GeoScope
  case object WorldScope extends GeoScope

  implicit class GeoScopeStringOps(s: String) {
    def toOptionalContinent(implicit enum: Enum[Continent]): Option[ContinentScope] =
      enum.withNameOption(s.replace(" ", "_")) map ContinentScope

    def toOptionalCountry(implicit enum: Enum[Country]): Option[CountryScope] =
      enum.withNameOption(s) map CountryScope
  }

  case class Ranking(categories: Map[Category, CategoryRanking]) extends AnyVal

  /*A CategoryRanking contains a list of application's package names */
  case class CategoryRanking(ranking: List[PackageName]) extends AnyVal

  case class RankingError(code: Int, message: String, status: String)

  type TryRanking = RankingError Xor Ranking

  case class Entry(
    packageName: PackageName,
    category: Category,
    ranking: Int
  )

  case class UnrankedApp(packageName: String)

  case class RankedApp(packageName: String, category: String, ranking: Option[Int])

  object Queries {

    val allFields = List("packageName", "category", "ranking")
    private[this] val fieldsStr = allFields.mkString(", ")

    private def tableOf(scope: GeoScope): String = {
      val suffix = scope match {
        case CountryScope(country) ⇒
          import Country._
          val acron = country match {
            case Spain ⇒ "es"
            case United_Kingdom ⇒ "gb"
            case United_States ⇒ "us"
          }
          s"country_$acron"
        case ContinentScope(continent) ⇒ continent.entryName.toLowerCase
        case WorldScope ⇒ "earth"
      }
      s"rankings_$suffix"
    }

    def getBy(scope: GeoScope): String =
      s"select * from ${tableOf(scope)}"

    def insertBy(scope: GeoScope): String =
      s"insert into ${tableOf(scope)}($fieldsStr) values (?,?,?)"

    def deleteBy(scope: GeoScope): String =
      s"delete from ${tableOf(scope)}"

    def createDeviceAppsTemporaryTableSql(tableName: String) =
      s"""
         |CREATE TEMP TABLE $tableName(
         |  packagename character varying(256) NOT NULL,
         |  PRIMARY KEY (packagename)
         |)
       """.stripMargin

    def dropDeviceAppsTemporaryTableSql(tableName: String) = s"DROP TABLE $tableName"

    def insertDeviceApps(tableName: String) =
      s"""
         |INSERT INTO $tableName(packageName)
         |VALUES (?)
       """.stripMargin

    def getRankedApps(scope: GeoScope, tableName: String) =
      s"""
         |SELECT A.packagename, R.category, R.ranking
         |FROM ${tableOf(scope)} as R INNER JOIN $tableName as A ON R.packagename=A.packagename
         |ORDER BY R.category, R.ranking
       """.stripMargin
  }

}
