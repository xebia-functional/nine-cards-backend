package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.Package
import cards.nine.googleplay.service.free.algebra.Cache._
import cards.nine.googleplay.service.free.interpreter.cache.CacheWrapper._
import cats.~>
import com.redis.RedisClient
import org.joda.time.{ DateTime, DateTimeZone }

import scalaz.concurrent.Task

object CacheInterpreter extends (Ops ~> WithRedisClient) {

  import CirceCoders._

  def getDefinedFullCardsFor(wrap: CacheWrapper[CacheKey, CacheVal], keys: List[CacheKey]) =
    wrap.mget(keys).collect { case CacheVal(Some(card)) ⇒ card }

  def apply[A](ops: Ops[A]): WithRedisClient[A] = ops match {

    case GetValid(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        val keys = List(CacheKey.resolved(pack), CacheKey.permanent(pack))
        wrap.findFirst(keys).flatMap(_.card)
      }

    case GetValidMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap: CacheWrapper[CacheKey, CacheVal] = CacheWrapper[CacheKey, CacheVal](client)

        val resolvedValues = getDefinedFullCardsFor(wrap, packages map CacheKey.resolved)

        val remainingPackages = packages diff (resolvedValues map (_.packageName))

        val permanentValues = getDefinedFullCardsFor(wrap, remainingPackages map CacheKey.permanent)

        resolvedValues ++ permanentValues
      }

    case PutResolved(card) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.put(CacheEntry.resolved(card))
      }

    case PutResolvedMany(cards) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.mput(cards map CacheEntry.resolved)
      }

    case MarkPending(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.put(CacheEntry.pending(pack))
      }

    case MarkPendingMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.mput(packages map CacheEntry.pending)
      }

    case UnmarkPending(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.delete(CacheKey.pending(pack))
      }

    case UnmarkPendingMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.delete(packages map CacheKey.pending)
      }

    case MarkError(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.put(CacheEntry.error(pack, DateTime.now(DateTimeZone.UTC)))
      }

    case MarkErrorMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        val values = packages map (p ⇒ CacheEntry.error(p, DateTime.now(DateTimeZone.UTC)))
        wrap.mput(values)
      }

    case ClearInvalid(pack) ⇒ client: RedisClient ⇒
      Task {
        val errorsPattern: JsonPattern = PObject(List(
          PString("package") → PString(pack.value),
          PString("keyType") → PString(KeyType.Error.entryName),
          PString("date") → PStar
        ))
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.matchKeys(errorsPattern).foreach(wrap.delete)
        wrap.delete(CacheKey.pending(pack))
      }

    case ClearInvalidMany(packages) ⇒ client: RedisClient ⇒
      Task {
        def errorsPattern(pack: Package): JsonPattern = PObject(List(
          PString("package") → PString(pack.value),
          PString("keyType") → PString(KeyType.Error.entryName),
          PString("date") → PStar
        ))

        val wrap = CacheWrapper[CacheKey, CacheVal](client)

        val jsonPatternList = packages map errorsPattern
        jsonPatternList foreach (pattern ⇒ wrap.delete(wrap.matchKeys(pattern)))
        wrap.delete(packages map CacheKey.pending)
      }

    case IsPending(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.get(CacheKey.pending(pack)).isDefined
      }

    case ListPending(num) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        val pendingPattern: JsonPattern = PObject(List(
          PString("package") → PStar,
          PString("keyType") → PString(KeyType.Pending.entryName),
          PString("date") → PNull
        ))
        wrap.matchKeys(pendingPattern)
          .take(num).map(_.`package`)
      }
  }

}