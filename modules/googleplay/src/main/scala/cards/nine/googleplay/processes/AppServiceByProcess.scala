package cards.nine.googleplay.processes

import cats.data.Xor
import cats.~>
import cards.nine.commons.TaskInstances._
import cards.nine.commons.config.NineCardsConfig.nineCardsConfiguration
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.service.free.{ JoinInterpreter, JoinServices }
import cards.nine.googleplay.service.free.{ algebra ⇒ Alg, interpreter ⇒ Inter }
import com.redis.{ RedisClient, RedisClientPool }
import org.http4s.client.{ Client ⇒ HttpClient }
import org.joda.time.{ DateTime, DateTimeZone }

import scalaz.concurrent.Task

case class AppServiceByProcess(
  redisPool: RedisClientPool,
  apiHttpClient: HttpClient,
  webHttpClient: HttpClient
)
  extends (AppRequest ⇒ Task[InfoError Xor FullCard]) {

  import withTypes._

  val process: CardsProcesses[JoinServices] = CardsProcesses.processes[JoinServices]

  val googleApiInt: Alg.GoogleApi.Ops ~> Task = {
    import Inter.googleapi._
    val interp = new Interpreter(nineCardsConfiguration.google.play.api)
    val toTask = new HttpToTask(apiHttpClient)
    interp andThen toTask
  }

  val cacheInt: Alg.Cache.Ops ~> Task = {
    import Inter.cache._
    val toTask = new RedisToTask(redisPool)
    CacheInterpreter andThen toTask
  }

  val webScrapperInt: Alg.WebScraper.Ops ~> Task = {
    import Inter.webscrapper._
    val interp = new Interpreter(nineCardsConfiguration.google.play.web)
    val toTask = new HttpToTask(webHttpClient)
    interp andThen toTask
  }

  val interpreter: JoinServices ~> Task = JoinInterpreter.interpreter(
    googleApiInt, cacheInt, webScrapperInt
  )

  override def apply(appRequest: AppRequest): Task[InfoError Xor FullCard] = {
    val pack: Package = appRequest.packageName
    val date = DateTime.now(DateTimeZone.UTC)
    process.getCard(appRequest.packageName, appRequest.marketAuth)
      .foldMap(interpreter)
      .map(xor ⇒ xor.bimap(_ ⇒ InfoError(pack.value), c ⇒ c))
  }

}

object withTypes {
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
}

