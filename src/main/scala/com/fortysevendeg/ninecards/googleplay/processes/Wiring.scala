package com.fortysevendeg.ninecards.googleplay.processes

import cats.~>
import cats.data.Xor
import com.fortysevendeg.extracats.XorTaskOrComposer
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item}
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter._
import com.redis.RedisClientPool
import org.http4s.client.blaze.PooledHttp1Client
import scalaz.concurrent.Task

class Wiring() {

  val redisClientPool: RedisClientPool = {
    val baseConfig = "ninecards.googleplay.redis"
    new RedisClientPool(
      host = getConfigValue(s"$baseConfig.host"),
      port = getConfigNumber(s"$baseConfig.port"),
      secret = getOptionalConfigValue(s"$baseConfig.secret")
    )
  }

  private[this] val apiHttpClient = PooledHttp1Client()
  private[this] val webHttpClient = PooledHttp1Client()

  val appCardService: AppServiceByProcess =
    new AppServiceByProcess(redisClientPool, apiHttpClient, webHttpClient)

  val apiServices: googleapi.ApiServices = {
    import googleapi._
    val recommendClient = PooledHttp1Client()
    val conf: Configuration = Configuration.load
    val client = new ApiClient( conf, recommendClient)
    new ApiServices(client, appCardService)
  }

  val itemService: (AppRequest => Task[String Xor Item]) = {
    val webClient = new Http4sGooglePlayWebScraper(
      getConfigValue("ninecards.googleplay.web.endpoint"),
      webHttpClient
    )
    new XorTaskOrComposer[AppRequest,String,Item](apiServices.getItem, webClient.getItem)
  }

  val interpreter: (GooglePlay.Ops ~> Task) = {
    new TaskInterpreter( itemService, appCardService,
      apiServices.recommendByCategory, apiServices.recommendByAppList )
  }

  def shutdown : Unit = {
    apiHttpClient.shutdownNow
    webHttpClient.shutdownNow
    redisClientPool.close
  }

}