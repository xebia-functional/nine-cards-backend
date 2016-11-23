package cards.nine.googleplay.processes

import cards.nine.commons.config.NineCardsConfig._
import cards.nine.googleplay.processes.GooglePlayApp.{ GooglePlayApp, Interpreters }
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cards.nine.googleplay.service.free.interpreter._
import cards.nine.googleplay.service.free.interpreter.cache.CacheInterpreter
import cats._
import com.redis.{ RedisClient, RedisClientPool }
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.{ Client ⇒ HttpClient }

import scalaz.concurrent.Task

object Wiring {

  type WithHttpClient[+A] = HttpClient ⇒ Task[A]
  type WithRedisClient[+A] = RedisClient ⇒ Task[A]

  class HttpToTask(httpClient: HttpClient)
    extends (WithHttpClient ~> Task) {
    override def apply[A](fa: WithHttpClient[A]): Task[A] = fa(httpClient)
  }

  class RedisToTask(redisPool: RedisClientPool)
    extends (WithRedisClient ~> Task) {
    override def apply[A](fa: WithRedisClient[A]): Task[A] = redisPool.withClient(fa)
  }

  private[this] val apiHttpClient = PooledHttp1Client()
  private[this] val webHttpClient = PooledHttp1Client()
  private[this] val redisClientPool: RedisClientPool = new RedisClientPool(
    host   = nineCardsConfiguration.redis.host,
    port   = nineCardsConfiguration.redis.port,
    secret = nineCardsConfiguration.redis.secret
  )

  val cacheInt: Cache.Ops ~> Task = {
    val toTask = new RedisToTask(redisClientPool)
    CacheInterpreter andThen toTask
  }

  val googleApiInt: GoogleApi.Ops ~> Task = {
    val interp = new googleapi.Interpreter(nineCardsConfiguration.google.play.api)
    val toTask = new HttpToTask(apiHttpClient)
    interp andThen toTask
  }

  val webScrapperInt: WebScraper.Ops ~> Task = {
    val interp = new webscrapper.Interpreter(nineCardsConfiguration.google.play.web)
    val toTask = new HttpToTask(webHttpClient)
    interp andThen toTask
  }

  val interpreters: GooglePlayApp ~> Task = Interpreters(googleApiInt, cacheInt, webScrapperInt)

  def shutdown(): Unit = {
    apiHttpClient.shutdownNow
    webHttpClient.shutdownNow
    redisClientPool.close
  }

}