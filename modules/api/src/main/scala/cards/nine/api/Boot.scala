package cards.nine.api

import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cards.nine.api.RankingActor.RankingByCategory
import cards.nine.commons.config.NineCardsConfig._
import spray.can.Http

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val log = Logging(system, getClass)

  // create and start our service actor
  val service = system.actorOf(Props[NineCardsApiActor], "ninecards-server")

  val rankingActor = system.actorOf(Props[RankingActor], "ninecards-server-ranking")

  val cancellable =
    system.scheduler.schedule(
      5.seconds,
      nineCardsConfiguration.rankings.actorInterval,
      rankingActor,
      RankingByCategory
    )

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(
    listener  = service,
    interface = nineCardsConfiguration.http.host,
    port      = nineCardsConfiguration.http.port
  )

  log.info("Application started!")
}