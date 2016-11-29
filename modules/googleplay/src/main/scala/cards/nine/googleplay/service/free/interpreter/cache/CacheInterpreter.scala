package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis._
import cards.nine.domain.application.Package
import cards.nine.googleplay.service.free.algebra.Cache._
import cats.~>
import cats.instances.list._
import cats.syntax.cartesian._
import cats.syntax.functor._
import cats.syntax.traverse._

import org.joda.time.{ DateTime, DateTimeZone }

object CacheInterpreter extends (Ops ~> RedisOps) {

  import Formats._
  import RedisOps._

  private[this] val wrap: CacheWrapper[CacheKey, CacheVal] = new CacheWrapper

  private[this] val errorCache: CacheQueue[CacheKey, DateTime] = new CacheQueue

  private[this] val pendingQueue: CacheQueue[PendingQueueKey.type, Package] = new CacheQueue

  def apply[A](ops: Ops[A]): RedisOps[A] = ops match {

    case GetValid(pack) ⇒
      val keys = List(CacheKey.resolved(pack), CacheKey.permanent(pack))
      wrap.mget(keys).map(_.flatMap(_.card).headOption)

    case GetValidMany(packages) ⇒
      val keys = (packages map CacheKey.resolved) ++ (packages map CacheKey.permanent)
      wrap.mget(keys).map(_.flatMap(_.card))

    case PutResolved(card) ⇒
      val pack = card.packageName
      removePending(pack) *> removeError(pack) *> wrap.put(CacheEntry.resolved(card))

    case PutResolvedMany(cards) ⇒
      val packs = cards.map(_.packageName)
      wrap.mput(cards map CacheEntry.resolved) *> removeErrorMany(packs) *> removePendingMany(packs)

    case PutPermanent(card) ⇒
      val pack = card.packageName
      removePending(pack) *> removeError(pack) *> wrap.put(CacheEntry.permanent(card))

    case SetToPending(pack) ⇒
      removeError(pack) *> addPending(pack) *> wrap.put(CacheEntry.pending(pack))

    case SetToPendingMany(packages) ⇒
      val enqueues = packages.traverse[RedisOps, Unit](addPending)
      val put = wrap.mput(packages map CacheEntry.pending)
      removeErrorMany(packages) *> enqueues *> put

    case AddError(pack) ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      removePending(pack) *> errorCache.enqueue(CacheKey.error(pack), now)

    case AddErrorMany(packages) ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      val putErrors = packages.traverse[RedisOps, Unit] {
        pack ⇒ errorCache.enqueue(CacheKey.error(pack), now)
      }
      putErrors *> removePendingMany(packages)

    case ListPending(num) ⇒
      val take = pendingQueue.takeMany(PendingQueueKey, num)
      val deque = pendingQueue.dequeueMany(PendingQueueKey, num)
      take <* deque
  }

  private[this] def addPending(pack: Package): RedisOps[Unit] = {
    val pendingKey = CacheKey.pending(pack)
    pendingQueue.enqueueIfNotExists[CacheKey](PendingQueueKey, pendingKey, pack)
  }

  private[this] def removePending(pack: Package): RedisOps[Unit] =
    pendingQueue.purge(PendingQueueKey, pack) *> wrap.delete(CacheKey.pending(pack))

  private[this] def removePendingMany(packages: List[Package]): RedisOps[Unit] = {
    val purge = packages traverse { p ⇒ pendingQueue.purge(PendingQueueKey, p) }
    purge *> wrap.delete(packages map CacheKey.pending)
  }

  private[this] def removeError(pack: Package): RedisOps[Unit] =
    errorCache.delete(CacheKey.error(pack))

  private[this] def removeErrorMany(packages: List[Package]): RedisOps[Unit] =
    errorCache.delete(packages map CacheKey.error)

}
