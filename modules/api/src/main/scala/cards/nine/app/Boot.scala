package cards.nine.app

import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cards.nine.app.RankingActor.RankingByCategory
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.processes.NineCardsServices
import cards.nine.processes.NineCardsServices.NineCardsServices
import cats.~>
import spray.can.Http
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaz.concurrent.Task

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
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

  val apiActor = system.actorOf(Props[NineCardsApiActor], "ninecards-server")

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

  IO(Http) ? Http.Bind(
    listener  = apiActor,
    interface = nineCardsConfiguration.http.host,
    port      = nineCardsConfiguration.http.port
  )

  log.info("Application started!")
}