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
import cards.nine.domain.application.Package
import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import freestyle._

@free trait Collection {
  def add(collection: SharedCollectionData): FS[Result[domain.SharedCollection]]
  def getById(id: Long): FS[Result[domain.SharedCollection]]
  def getByPublicId(publicId: String): FS[Result[domain.SharedCollection]]
  def getByUser(user: Long): FS[Result[List[domain.SharedCollectionWithAggregatedInfo]]]
  def getLatestByCategory(category: String, pageParams: Page): FS[Result[List[domain.SharedCollection]]]
  def getTopByCategory(category: String, pageParams: Page): FS[Result[List[domain.SharedCollection]]]
  def increaseViewsByOne(id: Long): FS[Result[Int]]
  def update(id: Long, title: String): FS[Result[Int]]
  def updatePackages(collection: Long, packages: List[Package]): FS[Result[(List[Package], List[Package])]]
}
