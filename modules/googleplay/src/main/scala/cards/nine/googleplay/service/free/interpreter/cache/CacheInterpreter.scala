package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis._
import cards.nine.googleplay.service.free.algebra.Cache._
import cats.~>
import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }
import io.circe.Encoder
import io.circe.parser._
import org.joda.time.{ DateTime, DateTimeZone }
import scalaz.concurrent.Task

object CacheInterpreter extends (Ops ~> WithRedisClient) {

  import CirceCoders._

  implicit val keyParse: Parse[Option[CacheKey]] =
    Parse(bv ⇒ KeyFormat.parse(Parse.Implicits.parseString(bv)))

  implicit val valParse: Parse[Option[CacheVal]] =
    Parse(bv ⇒ decode[CacheVal](Parse.Implicits.parseString(bv)).toOption)

  implicit def keyAndValFormat(implicit ev: Encoder[CacheVal]): Format =
    Format {
      case key: CacheKey ⇒ KeyFormat.format(key)
      case value: CacheVal ⇒ ev(value).noSpaces
    }

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
        PendingQueue(client).enqueue(PendingQueue.QueueKey, pack)
      }

    case MarkPendingMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.mput(packages map CacheEntry.pending)
        PendingQueue(client).enqueueMany(PendingQueue.QueueKey, packages)
      }

    case UnmarkPending(pack) ⇒ client: RedisClient ⇒
      Task {
        // We do not remove from PendingQueue: that would be too expensive
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.delete(CacheKey.pending(pack))
      }

    case UnmarkPendingMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.delete(packages map CacheKey.pending)
      }

    case MarkError(pack) ⇒ client: RedisClient ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      Task {
        ErrorCache(client).enqueue(CacheKey.error(pack), now)
      }

    case MarkErrorMany(packages) ⇒ client: RedisClient ⇒
      val now = DateTime.now(DateTimeZone.UTC)
      Task {
        ErrorCache(client)
          .enqueueAtMany(packages map CacheKey.error, now)
      }

    case ClearInvalid(pack) ⇒ client: RedisClient ⇒
      Task {
        CacheWrapper[CacheKey, CacheVal](client)
          .delete(CacheKey.pending(pack))
        ErrorCache(client).delete(CacheKey.error(pack))
      }

    case ClearInvalidMany(packages) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.delete(packages map CacheKey.pending)
        ErrorCache(client).delete(packages map CacheKey.error)
      }

    case IsPending(pack) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        wrap.get(CacheKey.pending(pack)).isDefined
      }

    case ListPending(num) ⇒ client: RedisClient ⇒
      Task {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)
        val queue = PendingQueue(client)
        val packs = queue.takeMany(PendingQueue.QueueKey, num)
        queue.dequeueMany(PendingQueue.QueueKey, num)
        wrap.delete(packs map CacheKey.pending)
        packs
      }
  }

}