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

import cards.nine.services.free.domain.User.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class UserQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val getUserByEmailQuery = userPersistence.generateQuery(
    sql    = getByEmail,
    values = "hello@47deg.com"
  )
  check(getUserByEmailQuery)

  val insertUserQuery = userPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = ("hello@47deg.com", "e1e938889-2e2d-49d7-81e7-10606c4ca32f", "7de44327-2e19-4a7f-b02f-cdc0c9d7c21c")
  )
  check(insertUserQuery)

}
