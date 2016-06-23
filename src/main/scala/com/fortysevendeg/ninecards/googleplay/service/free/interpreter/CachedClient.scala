package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.cache.redis.RedisCachedMonadicFunction
import com.fortysevendeg.extracats.taskMonad
import com.fortysevendeg.ninecards.googleplay.domain.Domain.{Item, Package}
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.redis.RedisClientPool
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import scalaz.concurrent.Task

class CachedQueryService(serviceName: String, baseService: QueryService, redisPool: RedisClientPool)
    extends RedisCachedMonadicFunction[QueryRequest, QueryResult, Task](baseService, redisPool){

  import io.circe._
  import io.circe.generic.auto._
  import io.circe.generic.semiauto._

  case class CachedQueryKey(serviceName: String, packageName: String, locale: Option[Localization])

  override protected[this] type CacheKey = CachedQueryKey
  override protected[this] type CacheVal = QueryResult

  override protected[this] val encodeKey: Encoder[CacheKey] = deriveEncoder[CachedQueryKey]
  override protected[this] val decodeVal: Decoder[CacheVal] = deriveDecoder[QueryResult]
  override protected[this] val encodeVal: Encoder[CacheVal] = deriveEncoder[QueryResult]

  override protected[this] def extractKey(query: QueryRequest): CacheKey =
    CachedQueryKey(serviceName, query._1.value, query._2._3)

  override protected[this] def extractValue(input: QueryRequest,result: QueryResult): QueryResult = result
  override protected[this] def rebuildValue(input: QueryRequest, value: QueryResult): QueryResult = value

}
