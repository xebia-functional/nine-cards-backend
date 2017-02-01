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

import cards.nine.services.free.domain.Installation.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class InstallationsQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val getByIdQuery = installationPersistence.generateQuery(
    sql    = getById,
    values = 1l
  )
  check(getByIdQuery)

  val getSubscribedByCollectionQuery = installationPersistence.generateQuery(
    sql    = getSubscribedByCollection,
    values = "111a-222b-33c-444d13"
  )
  check(getSubscribedByCollectionQuery)

  val getByUserAndAndroidIdQuery = installationPersistence.generateQuery(
    sql    = getByUserAndAndroidId,
    values = (1l, "111a-222b-33c-444d13")
  )
  check(getByUserAndAndroidIdQuery)

  val insertInstallationQuery = installationPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (1l, Option("111a-222b-4d13"), "35a4df64a31adf3")
  )
  check(insertInstallationQuery)

  val updateDeviceTokenQuery = installationPersistence.generateUpdateWithGeneratedKeys(
    sql    = updateDeviceToken,
    values = (Option("111a-222b-4d13"), 1l, "35a4df64a31adf3")
  )
  check(updateDeviceTokenQuery)

}