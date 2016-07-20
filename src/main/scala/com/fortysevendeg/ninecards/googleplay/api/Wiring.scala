package com.fortysevendeg.ninecards.googleplay.api

import cats.~>
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter._
import com.redis.RedisClientPool
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task

object Wiring {

  def interpreter(): GooglePlay.Ops ~> Task = {
    val httpClient = PooledHttp1Client()
    val redisPool = new RedisClientPool(
      host = getConfigValue("googleplay.cache.host"),
      port = getConfigNumber("googleplay.cache.port")
    )
    val apiClient = new Http4sGooglePlayApiClient(  getConfigValue("googleplay.api.endpoint"), httpClient)
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint"), httpClient)
    val cachedApiClient = new CachedAppService( "apiClient", apiClient, redisPool)
    val cachedWebScrape = new CachedAppService( "webScrape", webClient, redisPool)
    TaskInterpreter(cachedApiClient, cachedWebScrape)
  }

}