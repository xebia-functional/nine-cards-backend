package cards.nine.services.free.domain

import cards.nine.domain.application.{ Category, Package }
import cards.nine.domain.analytics.{ Country ⇒ ACountry, _ }
import cats.data.Xor

object rankings {

  case class RankingParams(
    dateRange: DateRange,
    rankingLength: Int,
    auth: AnalyticsToken
  )

  case class Ranking(categories: Map[Category, CategoryRanking]) extends AnyVal

  case class UpdateRankingSummary(created: Int, deleted: Int)

  /*A CategoryRanking contains a list of application's package names */
  case class CategoryRanking(ranking: List[Package]) extends AnyVal

  case class RankingError(code: Int, message: String, status: String)

  type TryRanking = RankingError Xor Ranking

  case class Entry(
    packageName: Package,
    category: Category,
    ranking: Int
  )

  case class UnrankedApp(packageName: Package, category: String)

  case class RankedApp(packageName: Package, category: String, ranking: Option[Int])

  object Queries {

    val allFields = List("packageName", "category", "ranking")
    private[this] val fieldsStr = allFields.mkString(", ")

    private def tableOf(scope: GeoScope): String = {
      val suffix = scope match {
        case CountryScope(country) ⇒
          import ACountry._
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
         |  category character varying(64) NOT NULL,
         |  PRIMARY KEY (packagename, category)
         |)
       """.stripMargin

    def dropDeviceAppsTemporaryTableSql(tableName: String) = s"DROP TABLE $tableName"

    def insertDeviceApps(tableName: String) =
      s"""
         |INSERT INTO $tableName(packageName, category)
         |VALUES (?,?)
       """.stripMargin

    def getRankedApps(scope: GeoScope, tableName: String, moments: List[String]) =
      s"""
         |SELECT A.packagename, R.category, R.ranking
         |FROM ${tableOf(scope)} as R INNER JOIN $tableName as A ON R.packagename=A.packagename
         |WHERE R.category=A.category OR R.category IN (${moments.map(m ⇒ s"'$m'").mkString(",")})
         |ORDER BY R.category, R.ranking
       """.stripMargin
  }

}
