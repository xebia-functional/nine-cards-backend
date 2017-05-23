/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  val googleApiInt: GoogleApi.Op ~> Task = {
    val interp = new googleapi.Interpreter(config.google.play.api)
    val toTask = new HttpToTask(apiHttpClient)
    interp andThen toTask
  }

  private[this] val webHttpClient = PooledHttp1Client()

  val webScrapperInt: WebScraper.Op ~> Task = {
    val interp = new webscrapper.Interpreter(config.google.play.web)
    val toTask = new HttpToTask(webHttpClient)
    interp andThen toTask
  }

  val cacheInt: Cache.Op ~> Task = {
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
