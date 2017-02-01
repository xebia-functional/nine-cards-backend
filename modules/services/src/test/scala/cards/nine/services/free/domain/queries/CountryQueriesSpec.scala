/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.domain.queries

import cards.nine.services.free.domain.Country.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class CountryQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val getAllQuery = countryPersistence.generateQuery(
    sql = getAllSql
  )
  check(getAllQuery)

  val getByIsoCode2Query = countryPersistence.generateQuery(
    sql    = getByIsoCode2Sql,
    values = "US"
  )
  check(getByIsoCode2Query)
}
