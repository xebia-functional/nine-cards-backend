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
package cards.nine.processes.collections

import java.sql.Timestamp

import cards.nine.domain.application.Package
import cards.nine.processes.collections.messages._
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionWithAggregatedInfo, SharedCollection ⇒ SharedCollectionServices, SharedCollectionSubscription ⇒ SharedCollectionSubscriptionServices }
import cards.nine.services.free.interpreter.collection.Services.{ SharedCollectionData ⇒ SharedCollectionDataServices }
import org.joda.time.DateTime

private[collections] object Converters {

  def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  def toTimestamp(datetime: DateTime): Timestamp = new Timestamp(datetime.getMillis)

  def toSharedCollectionDataServices(
    data: SharedCollectionData
  ): SharedCollectionDataServices =
    SharedCollectionDataServices(
      publicIdentifier = data.publicIdentifier,
      userId           = data.userId,
      publishedOn      = toTimestamp(data.publishedOn),
      author           = data.author,
      name             = data.name,
      views            = data.views.getOrElse(0),
      category         = data.category,
      icon             = data.icon,
      community        = data.community,
      packages         = data.packages map (_.value)
    )

  def toSharedCollectionList(userId: Long)(collections: List[BaseSharedCollection]): List[SharedCollection] =
    collections map (col ⇒ toSharedCollection(col, userId))

  def toSharedCollection(collection: BaseSharedCollection, userId: Long): SharedCollection =
    collection match {
      case (c: SharedCollectionWithAggregatedInfo) ⇒
        toSharedCollection(c.sharedCollectionData, Option(c.subscriptionsCount), userId)
      case (c: SharedCollectionServices) ⇒
        toSharedCollection(c, None, userId)
    }

  def toSharedCollection(
    collection: SharedCollectionServices,
    subscriptionCount: Option[Long],
    userId: Long
  ) =
    SharedCollection(
      publicIdentifier   = collection.publicIdentifier,
      publishedOn        = toJodaDateTime(collection.publishedOn),
      author             = collection.author,
      name               = collection.name,
      views              = collection.views,
      category           = collection.category,
      icon               = collection.icon,
      community          = collection.community,
      owned              = collection.userId.fold(false)(user ⇒ user == userId),
      packages           = collection.packages map Package,
      subscriptionsCount = subscriptionCount
    )

  def toSharedCollectionWithAppsInfo[A](
    collection: SharedCollection,
    appsInfo: List[A]
  ): SharedCollectionWithAppsInfo[A] =
    SharedCollectionWithAppsInfo(
      collection = collection,
      appsInfo   = appsInfo
    )

  def toGetSubscriptionsByUserResponse(subscriptions: List[SharedCollectionSubscriptionServices]) =
    GetSubscriptionsByUserResponse(
      subscriptions map (_.sharedCollectionPublicId)
    )

}
