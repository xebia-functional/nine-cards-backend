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
package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import enumeratum.{ Enum, EnumEntry }

sealed trait KeyType extends EnumEntry
object KeyType extends Enum[KeyType] {
  case object Resolved extends KeyType
  case object Permanent extends KeyType
  case object Error extends KeyType

  val values = super.findValues
}

case class CacheKey(`package`: Package, keyType: KeyType)

object CacheKey {
  import KeyType._

  def resolved(name: Package): CacheKey = CacheKey(name, Resolved)

  def permanent(name: Package): CacheKey = CacheKey(name, Permanent)

  def error(name: Package): CacheKey = CacheKey(name, Error)
}

case class CacheVal(card: Option[FullCard])

object CacheEntry {

  def resolved(card: FullCard): (CacheKey, CacheVal) =
    CacheKey.resolved(card.packageName) → CacheVal(Some(card))

  def permanent(card: FullCard): (CacheKey, CacheVal) =
    CacheKey.permanent(card.packageName) → CacheVal(Some(card))
}

object PendingQueueKey

