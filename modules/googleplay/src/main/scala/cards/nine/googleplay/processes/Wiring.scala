package cards.nine.googleplay.processes

import cards.nine.googleplay.config.NineCardsConfig._
import cards.nine.googleplay.domain.{AppRequest, Item}
import cards.nine.googleplay.extracats.XorTaskOrComposer
import cards.nine.googleplay.service.free.algebra.GooglePlay
import cards.nine.googleplay.service.free.interpreter._
import cards.nine.googleplay.service.free.interpreter.webscrapper.Http4sGooglePlayWebScraper
import cats.data.Xor
import cats.~>
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
    val byCategory = { (message: GooglePlay.RecommendationsByCategory) =>
      apiServices.recommendByCategory(message.request, message.auth)
    }
    val byAppList = { (message: GooglePlay.RecommendationsByAppList) =>
      apiServices.recommendByAppList(message.request, message.auth)
    }
    new TaskInterpreter( itemService, appCardService, byCategory, byAppList)
  }

  def shutdown : Unit = {
    apiHttpClient.shutdownNow
    webHttpClient.shutdownNow
    redisClientPool.close
  }

}