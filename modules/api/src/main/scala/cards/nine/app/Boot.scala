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
package cards.nine.app

import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.util.Timeout
import cards.nine.app.RankingActor.RankingByCategory
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.processes.NineCardsServices
import cards.nine.processes.NineCardsServices.NineCardsServices
import cats.~>
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaz.concurrent.Task

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-akka-http")
  implicit val materializer: Materializer = ActorMaterializer()

  // the background processes will log on same output
  val log = Logging(system, getClass)

  implicit val timeout = Timeout(5.seconds)

  val interpreter: NineCardsServices ~> Task = NineCardsServices.prodInterpreters

  val rankingActor = system.actorOf(
    props = Props(new RankingActor[NineCardsServices](interpreter, log)),
    name  = "ninecards-server-ranking"
  )

  val appResolverActor = system.actorOf(
    props = Props(new AppResolverActor[NineCardsServices](interpreter, log)),
    name  = "ninecards-server-apps-resolver"
  )

  val apiActor = new NineCardsApiActor()

  val cancellable =
    system.scheduler.schedule(
      5.seconds,
      nineCardsConfiguration.rankings.actorInterval,
      rankingActor,
      RankingByCategory
    )

  val cancellablePending =
    system.scheduler.schedule(
      20.seconds,
      nineCardsConfiguration.google.play.resolveInterval,
      appResolverActor,
      AppResolverMessages.ResolveApps
    )

  Http().bindAndHandle(
    handler   = apiActor.routes,
    interface = nineCardsConfiguration.http.host,
    port      = nineCardsConfiguration.http.port
  )

  log.info("Application started!")
}