package com.fortysevendeg.ninecards.googleplay.api

import cats.~>
import com.fortysevendeg.extracats.XorTaskOrComposer
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item, AppCard}
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter._
import com.redis.RedisClientPool
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task

object Wiring {

  def interpreter(): GooglePlay.Ops ~> Task = {
    val httpClient = PooledHttp1Client()
    val apiClient = new Http4sGooglePlayApiClient(  getConfigValue("googleplay.api.endpoint"), httpClient)
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint"), httpClient)
    val redisPool = new RedisClientPool(
      host = getConfigValue("googleplay.cache.host"),
      port = getConfigNumber("googleplay.cache.port")
    )
    val itemService = new XorTaskOrComposer[AppRequest,String,Item](
      new CachedItemService( "apiClient_item", apiClient.getItem, redisPool),
      new CachedItemService( "webScrape_item", webClient.getItem, redisPool)
    )
    val appService = new XorTaskOrComposer[AppRequest,String,AppCard](
      new CachedAppService( "apiClient_app", apiClient.getCard, redisPool),
      new CachedAppService( "webScrape_app", webClient.getCard, redisPool)
    )
    new TaskInterpreter(itemService, appService)
  }

}