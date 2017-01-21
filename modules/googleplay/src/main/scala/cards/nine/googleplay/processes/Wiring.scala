package cards.nine.googleplay.processes

import akka.actor.ActorSystem
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.commons.redis.RedisOpsToTask
import cards.nine.googleplay.processes.GooglePlayApp.{ GooglePlayApp, Interpreters }
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cards.nine.googleplay.service.free.interpreter._
import cards.nine.googleplay.service.free.interpreter.cache.CacheInterpreter
import cats._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.client.{ Client ⇒ HttpClient }
import scalaz.concurrent.Task
import scala.concurrent.ExecutionContext

class Wiring(
  config: NineCardsConfiguration
)(
  implicit
  actorSystem: ActorSystem, ec: ExecutionContext
) extends (GooglePlayApp ~> Task) {

  type WithHttpClient[+A] = HttpClient ⇒ Task[A]

  class HttpToTask(httpClient: HttpClient)
    extends (WithHttpClient ~> Task) {
    override def apply[A](fa: WithHttpClient[A]): Task[A] = fa(httpClient)
  }

  private[this] val apiHttpClient = PooledHttp1Client(
    maxTotalConnections = config.google.play.api.detailsBatchSize
  )

  val googleApiInt: GoogleApi.Ops ~> Task = {
    val interp = new googleapi.Interpreter(config.google.play.api)
    val toTask = new HttpToTask(apiHttpClient)
    interp andThen toTask
  }

  private[this] val webHttpClient = PooledHttp1Client()

  val webScrapperInt: WebScraper.Ops ~> Task = {
    val interp = new webscrapper.Interpreter(config.google.play.web)
    val toTask = new HttpToTask(webHttpClient)
    interp andThen toTask
  }

  val cacheInt: Cache.Ops ~> Task = {
    val toTask = RedisOpsToTask(config.redis)
    new CacheInterpreter() andThen toTask
  }

  private[this] val interpreters: GooglePlayApp ~> Task = Interpreters(googleApiInt, cacheInt, webScrapperInt)

  def apply[A](ops: GooglePlayApp[A]): Task[A] = interpreters(ops)

  def shutdown(): Unit = {
    apiHttpClient.shutdownNow
    webHttpClient.shutdownNow
  }

}
