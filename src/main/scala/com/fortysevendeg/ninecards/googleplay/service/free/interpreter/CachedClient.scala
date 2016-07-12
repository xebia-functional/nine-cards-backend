package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import com.fortysevendeg.cache.redis.RedisCachedMonadicFunction
import com.fortysevendeg.extracats.taskMonad
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item, Localization, Package}
import com.redis.RedisClientPool
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import scalaz.concurrent.Task

class CachedAppService(serviceName: String, baseService: AppService, redisPool: RedisClientPool)
    extends RedisCachedMonadicFunction[AppRequest, Xor[String, Item], Task](baseService, redisPool){

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.generic.semiauto._

  case class CachedQueryKey(serviceName: String, packageName: String, locale: Option[Localization])

  override protected[this] type CacheKey = CachedQueryKey
  override protected[this] type CacheVal = Xor[String, Item]

  override protected[this] val encodeKey: Encoder[CacheKey] = deriveEncoder[CachedQueryKey]
  override protected[this] val decodeVal: Decoder[CacheVal] = deriveDecoder[CacheVal]
  override protected[this] val encodeVal: Encoder[CacheVal] = deriveEncoder[CacheVal]

  override protected[this] def extractKey(query: AppRequest): CacheKey =
    CachedQueryKey(serviceName, query.packageName.value, query.authParams.localization)

  override protected[this] def extractValue(input: AppRequest,result: CacheVal): CacheVal = result
  override protected[this] def rebuildValue(input: AppRequest, value: CacheVal): CacheVal = value

}
