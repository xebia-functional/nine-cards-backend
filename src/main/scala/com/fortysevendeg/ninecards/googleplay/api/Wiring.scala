package com.fortysevendeg.ninecards.googleplay.api

import cats.~>
import com.fortysevendeg.extracats.XorTaskOrComposer
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import com.fortysevendeg.ninecards.googleplay.domain.{FullCard, AppRequest, InfoError, Item}
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache.{ CachedAppService, CachedItemService}
import com.redis.RedisClientPool
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task

object Wiring {

  val configBase: String = "ninecards.googleplay"

  private[this] val googleApiConf: googleapi.Configuration = googleapi.Configuration.load()

  def interpreter(): GooglePlay.Ops ~> Task = {
    val httpClient = PooledHttp1Client()
    val apiServices = new googleapi.ApiServices(
      new googleapi.ApiClient( googleApiConf, httpClient)
    )
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("ninecards.googleplay.web.endpoint"), httpClient)
    val redisPool = new RedisClientPool(
      host = getConfigValue("ninecards.googleplay.redis.host"),
      port = getConfigNumber("ninecards.googleplay.redis.port"),
      secret = getOptionalConfigValue("ninecards.googleplay.redis.secret")
    )
    val itemService = new XorTaskOrComposer[AppRequest,String,Item](
      new CachedItemService( apiServices.getItem, redisPool),
      new CachedItemService( webClient.getItem, redisPool)
    )
    val appService = new XorTaskOrComposer[AppRequest,InfoError,FullCard](
      new CachedAppService( apiServices.getCard, redisPool),
      new CachedAppService( webClient.getCard, redisPool)
    )
    new TaskInterpreter(itemService, appService, apiServices.recommendByCategory, apiServices.recommendByAppList)
  }

}