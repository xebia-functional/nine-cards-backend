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
package cards.nine.processes.applications

import cards.nine.domain.analytics._
import cards.nine.domain.application.{ CardList, FullCard, Package }

private[applications] object Converters {

  def filterCategorized(info: CardList[FullCard]): CardList[FullCard] = {
    val (appsWithoutCategories, apps) = info.cards.partition(app ⇒ app.categories.isEmpty)
    CardList(
      missing = info.missing ++ appsWithoutCategories.map(_.packageName),
      pending = info.pending,
      cards   = apps
    )
  }

  def toUnrankedApp(category: String)(pack: Package) = UnrankedApp(pack, category)

  def toRankedAppsByCategory(limit: Option[Int] = None)(ranking: (String, List[RankedApp])) = {
    val (category, apps) = ranking

    RankedAppsByCategory(category, limit.fold(apps)(apps.take))
  }

}
