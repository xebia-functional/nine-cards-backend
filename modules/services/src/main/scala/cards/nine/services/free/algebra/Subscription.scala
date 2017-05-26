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
package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService.Result
import cards.nine.services.free.domain.SharedCollectionSubscription
import freestyle._

@free trait SubscriptionR {
  def add(collection: Long, user: Long, collectionPublicId: String): FS[Result[Int]]
  def getByCollectionAndUser(collection: Long, user: Long): FS[Result[Option[SharedCollectionSubscription]]]
  def getByCollection(collection: Long): FS[Result[List[SharedCollectionSubscription]]]
  def getByUser(user: Long): FS[Result[List[SharedCollectionSubscription]]]
  def removeByCollectionAndUser(collection: Long, user: Long): FS[Result[Int]]
}
