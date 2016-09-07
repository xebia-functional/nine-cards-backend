package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import cats.data.Xor
import com.fortysevendeg.extracats
import com.fortysevendeg.ninecards.googleplay.domain._
import com.redis.RedisClientPool
import scalaz.concurrent.Task

class CachedAppService(
  baseService: AppRequest => Task[Xor[InfoError,FullCard]],
  redisPool: RedisClientPool)
    extends RedisCachedMonadicFunction[AppRequest, Xor[InfoError, FullCard], Task](baseService, redisPool)(extracats.taskMonad){

  import io.circe._

  override protected[this] type Key = CacheKey
  override protected[this] type Val = CacheVal

  override protected[this] val encodeKey: Encoder[CacheKey] = CirceCoders.cacheKeyE
  override protected[this] val decodeVal: Decoder[CacheVal] = CirceCoders.cacheValD
  override protected[this] val encodeVal: Encoder[CacheVal] = CirceCoders.cacheValE

  private[this] def pendingKey(input: AppRequest) = CacheKey( input.packageName, KeyType.Resolved, None)
  private[this] def resolveKey(input: AppRequest) = CacheKey( input.packageName, KeyType.Pending, None)

  override protected[this] def extractKeys(query: AppRequest): List[CacheKey] =
    List( resolveKey(query), pendingKey(query) )

  override protected[this] def extractEntry(input: AppRequest, result: Xor[InfoError, FullCard]) : (Key, Val) =
    result match {
      case Xor.Right(fullCard) => ( resolveKey(input), CacheVal( Some( fullCard) ) )
      case Xor.Left(infoError) => ( pendingKey(input), CacheVal( None) )
    }

  override protected[this] def rebuildValue(input: AppRequest, cacheVal: CacheVal): Xor[InfoError, FullCard] =
    Xor.fromOption( cacheVal.card, InfoError("Not in the cache") )

}

class CachedItemService(
  baseService: AppRequest => Task[Xor[String,Item]],
  redisPool: RedisClientPool
) extends RedisCachedMonadicFunction[AppRequest, Xor[String, Item], Task](
  baseService, redisPool)(extracats.taskMonad
){

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.generic.semiauto._

  case class QueryKey(packageName: String)

  override protected[this] type Key = QueryKey
  override protected[this] type Val = Xor[String, Item]

  override protected[this] val encodeKey: Encoder[Key] = deriveEncoder[QueryKey]
  override protected[this] val decodeVal: Decoder[Val] = deriveDecoder[Val]
  override protected[this] val encodeVal: Encoder[Val] = deriveEncoder[Val]

  private[this] def toKey( input: AppRequest) = QueryKey(input.packageName.value)

  override protected[this] def extractKeys(input: AppRequest): List[Key] = List( toKey(input) )

  override protected[this] def extractEntry(input: AppRequest,result: Val): (Key, Val) = ( toKey(input), result)

  override protected[this] def rebuildValue(input: AppRequest, value: Val): Val = value

}
