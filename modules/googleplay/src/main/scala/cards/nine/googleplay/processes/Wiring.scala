package cards.nine.googleplay.processes

import akka.actor.ActorSystem
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.commons.redis.RedisOpsToTask
import cards.nine.googleplay.processes.GooglePlayApp.{ GooglePlayApp, Interpreters }
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cards.nine.googleplay.service.free.interpreter._
import cards.nine.googleplay.service.free.interpreter.cache.CacheInterpreter
import cats._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.{ Client ⇒ HttpClient }
import scalaz.concurrent.Task
import scredis.{ Client ⇒ RedisClient }

object Wiring {

  type WithHttpClient[+A] = HttpClient ⇒ Task[A]

  class HttpToTask(httpClient: HttpClient)
    extends (WithHttpClient ~> Task) {
    override def apply[A](fa: WithHttpClient[A]): Task[A] = fa(httpClient)
  }

  private[this] val apiHttpClient = PooledHttp1Client()
  private[this] val webHttpClient = PooledHttp1Client()

  implicit val system: ActorSystem = ActorSystem("cards-nine-googleplay-processes-Wiring")

  val redisClient: RedisClient = RedisClient(
    host        = nineCardsConfiguration.redis.host,
    port        = nineCardsConfiguration.redis.port,
    passwordOpt = nineCardsConfiguration.redis.secret
  )

  val cacheInt: Cache.Ops ~> Task = {
    val toTask = new RedisOpsToTask(redisClient)
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
  }

}
