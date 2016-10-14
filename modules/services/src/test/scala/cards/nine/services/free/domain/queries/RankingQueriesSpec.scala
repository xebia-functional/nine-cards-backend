package cards.nine.services.free.domain.queries

import cards.nine.domain.analytics._
import cards.nine.domain.application.Category
import cards.nine.services.free.domain.PackageName
import cards.nine.services.free.domain.rankings._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import doobie.imports.{ Query0, Update0 }
import org.specs2.mutable.Specification

class RankingQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  import cards.nine.services.persistence.CustomComposite._

  val data: Entry = Entry(PackageName("hammer"), Category.TOOLS, 17)

  val deviceAppData: UnrankedApp = UnrankedApp("com.package.name", "SOCIAL")

  val temporaryTableName = "device_apps"

  def getBy(scope: GeoScope): Query0[Entry] =
    rankingPersistence.generateQuery(Queries.getBy(scope))

  Country.values foreach { c ⇒ check(getBy(CountryScope(c))) }
  Continent.values foreach { c ⇒ check(getBy(ContinentScope(c))) }
  check(getBy(WorldScope))

  def deleteBy(scope: GeoScope): Update0 =
    rankingPersistence.generateUpdate(Queries.deleteBy(scope))

  Country.values foreach { c ⇒ check(deleteBy(CountryScope(c))) }
  Continent.values foreach { c ⇒ check(deleteBy(ContinentScope(c))) }
  check(deleteBy(WorldScope))

  def insertBy(scope: GeoScope): Update0 = {
    import shapeless.syntax.std.product._
    rankingPersistence.generateUpdateWithGeneratedKeys(
      sql    = Queries.insertBy(scope),
      values = data.toTuple
    )
  }

  Country.values foreach { c ⇒ check(insertBy(CountryScope(c))) }
  Continent.values foreach { c ⇒ check(insertBy(ContinentScope(c))) }
  check(insertBy(WorldScope))

  val createDeviceAppsTableQuery = rankingPersistence.generateUpdate(
    sql = Queries.createDeviceAppsTemporaryTableSql(temporaryTableName)
  )
  check(createDeviceAppsTableQuery)

  val dropDeviceAppsTableQuery = rankingPersistence.generateUpdate(
    sql = Queries.dropDeviceAppsTemporaryTableSql(temporaryTableName)
  )
  check(dropDeviceAppsTableQuery)

}
