package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cats.~>
import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.GooglePlayOps
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter._
import com.redis.RedisClientPool
import org.http4s.client.blaze.PooledHttp1Client
import scala.concurrent.duration._
import scalaz.concurrent.Task
import spray.can.Http

object Boot extends App {

  implicit private val system = ActorSystem("nine-cards-google-play-server-actor")

  private val interpreter: GooglePlayOps ~> Task = {
    val httpClient = PooledHttp1Client()
    val redisPool = new RedisClientPool(
      host = getConfigValue("googleplay.cache.host"),
      port = getConfigValue("googleplay.cache.port").toInt
    )
    val apiClient = new Http4sGooglePlayApiClient(  getConfigValue("googleplay.api.endpoint"), httpClient)
    val webClient = new Http4sGooglePlayWebScraper( getConfigValue("googleplay.web.endpoint"), httpClient)
    val cachedApiClient = new CachedAppService( "apiClient", apiClient, redisPool)
    val cachedWebScrape = new CachedAppService( "webScrape", webClient, redisPool)
    TaskInterpreter(cachedApiClient, cachedWebScrape)
  }

  private val service = system.actorOf(
    Props( classOf[NineCardsGooglePlayActor], interpreter),
    "nine-cards-google-play-server"
  )

  implicit private val timeout = Timeout(5.seconds)

  private val host = getConfigValue("ninecards.host")
  private val port = getConfigValue("ninecards.port").toInt

  IO(Http) ? Http.Bind(service, interface = host, port = port)
}
