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
      wrap.put(CacheEntry.resolved(card))

    case PutResolvedMany(cards) ⇒
      wrap.mput(cards map CacheEntry.resolved)

    case PutPermanent(card) ⇒
      wrap.put(CacheEntry.permanent(card))

    case MarkPending(pack) ⇒
      val a = wrap.put(CacheEntry.pending(pack))
      val b = pendingQueue.enqueue(PendingQueueKey, pack)
      a *> b

    case MarkPendingMany(packages) ⇒
      val a = wrap.mput(packages map CacheEntry.pending)
      val b = pendingQueue.enqueueMany(PendingQueueKey, packages)
      a *> b

    case UnmarkPending(pack) ⇒
      val a = wrap.delete(CacheKey.pending(pack))
      val b = pendingQueue.purge(PendingQueueKey, pack)
      a *> b

    case UnmarkPendingMany(packages) ⇒
      val a = wrap.delete(packages map CacheKey.pending)
      val b = packages.traverse[RedisOps, Unit] { pack ⇒
        pendingQueue.purge(PendingQueueKey, pack)
      }
      a <* b

    case MarkError(pack) ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      errorCache.enqueue(CacheKey.error(pack), now)

    case MarkErrorMany(packages) ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      packages.traverse[RedisOps, Unit] { pack ⇒
        errorCache.enqueue(CacheKey.error(pack), now)
      }.map(x ⇒ {})

    case ClearInvalid(pack) ⇒
      val a = wrap.delete(CacheKey.pending(pack))
      val b = errorCache.delete(CacheKey.error(pack))
      val c = pendingQueue.purge(PendingQueueKey, pack)
      a *> b *> c

    case ClearInvalidMany(packages) ⇒
      val pendings = packages map CacheKey.pending
      val errors = packages map CacheKey.error
      wrap.delete(pendings) *> errorCache.delete(errors)

    case IsPending(pack) ⇒
      wrap.get(CacheKey.pending(pack)).map(_.isDefined)

    case ListPending(num) ⇒
      val take = pendingQueue.takeMany(PendingQueueKey, num)
      val deque = pendingQueue.dequeueMany(PendingQueueKey, num)
      take <* deque

  }

}