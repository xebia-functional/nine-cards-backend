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

import cards.nine.commons.redis._
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.service.free.algebra.Cache
import cats.instances.list._
import cats.syntax.cartesian._
import cats.syntax.functor._
import cats.syntax.traverse._
import org.joda.time.{ DateTime, DateTimeZone }
import scala.concurrent.ExecutionContext

class CacheInterpreter(implicit ec: ExecutionContext) extends Cache.Handler[RedisOps] {

  import Formats._
  import RedisOps._

  private[this] val wrap: CacheWrapper[CacheKey, CacheVal] = new CacheWrapper

  private[this] val errorCache: CacheQueue[CacheKey, DateTime] = new CacheQueue

  private[this] val pendingSet: CacheSet[PendingQueueKey.type, Package] = new CacheSet(PendingQueueKey)

  override def getValid(pack: Package): RedisOps[Option[FullCard]] = {
    val keys = List(CacheKey.resolved(pack), CacheKey.permanent(pack))
    wrap.mget(keys).map(_.flatMap(_.card).headOption)
  }

  override def getValidMany(packages: List[Package]) = {
    val keys = (packages map CacheKey.resolved) ++ (packages map CacheKey.permanent)
    wrap.mget(keys).map(_.flatMap(_.card))
  }

  override def putResolved(card: FullCard): RedisOps[Unit] = {
    val pack = card.packageName
    pendingSet.remove(pack) *> removeError(pack) *> wrap.put(CacheEntry.resolved(card))
  }

  override def putResolvedMany(cards: List[FullCard]): RedisOps[Unit] = {
    val packs = cards.map(_.packageName)
    wrap.mput(cards map CacheEntry.resolved) *> removeErrorMany(packs) *> pendingSet.remove(packs)
  }

  override def putPermanent(card: FullCard): RedisOps[Unit] = {
    val pack = card.packageName
    pendingSet.remove(pack) *> removeError(pack) *> wrap.put(CacheEntry.permanent(card))
  }

  override def setToPending(pack: Package): RedisOps[Unit] =
    removeError(pack) *> pendingSet.insert(pack)

  override def setToPendingMany(packages: List[Package]): RedisOps[Unit] =
    removeErrorMany(packages) *> pendingSet.insert(packages)

  override def addError(pack: Package): RedisOps[Unit] = {
    val now = DateTime.now(DateTimeZone.UTC)
    pendingSet.remove(pack) *> errorCache.enqueue(CacheKey.error(pack), now)
  }

  override def addErrorMany(packages: List[Package]): RedisOps[Unit] = {
    val now = DateTime.now(DateTimeZone.UTC)
    val putErrors = packages.traverse[RedisOps, Unit] {
      pack ⇒ errorCache.enqueue(CacheKey.error(pack), now)
    }
    putErrors *> pendingSet.remove(packages)
  }

  override def listPending(num: Int): RedisOps[List[Package]] = pendingSet.extractMany(num)

  private[this] def removeError(pack: Package): RedisOps[Unit] =
    errorCache.delete(CacheKey.error(pack))

  private[this] def removeErrorMany(packages: List[Package]): RedisOps[Unit] =
    errorCache.delete(packages map CacheKey.error)

}
