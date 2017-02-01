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
package cards.nine.api.collections

import cards.nine.domain.application.Package
import cards.nine.processes.collections.messages._
import org.joda.time.DateTime

package messages {

  case class ApiCreateCollectionRequest(
    author: String,
    name: String,
    installations: Option[Int] = None,
    views: Option[Int] = None,
    category: String,
    icon: String,
    community: Boolean,
    packages: List[Package]
  )

  case class ApiCreateOrUpdateCollectionResponse(
    publicIdentifier: String,
    packagesStats: PackagesStats
  )

  case class ApiIncreaseViewsCountByOneResponse(
    publicIdentifier: String
  )

  case class ApiSharedCollection(
    publicIdentifier: String,
    publishedOn: DateTime,
    author: String,
    name: String,
    views: Int,
    category: String,
    icon: String,
    community: Boolean,
    owned: Boolean,
    packages: List[Package],
    appsInfo: List[ApiCollectionApp],
    subscriptions: Option[Long] = None
  )

  // AppCollectionApp: FullCard without Screenshots
  case class ApiCollectionApp(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class ApiSharedCollectionList(collections: List[ApiSharedCollection])

  case class ApiUpdateCollectionRequest(
    collectionInfo: Option[SharedCollectionUpdateInfo],
    packages: Option[List[Package]]
  )

  case class ApiGetSubscriptionsByUser(subscriptions: List[String])

  case class ApiSubscribeResponse()

  case class ApiUnsubscribeResponse()

}
