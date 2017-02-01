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

import cards.nine.services.free.domain.SharedCollectionSubscription.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class SharedCollectionSubscriptionQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val collectionId = 12345l
  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"
  val userId = 34567l

  val getByCollectionQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollection,
    values = collectionId
  )
  check(getByCollectionQuery)

  val getByCollectionAndUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(getByCollectionAndUserQuery)

  val getByUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByUser,
    values = userId
  )
  check(getByUserQuery)

  val insertQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (collectionId, userId, publicIdentifier)
  )
  check(insertQuery)

  val deleteByCollectionAndUserQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = deleteByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(deleteByCollectionAndUserQuery)
}
