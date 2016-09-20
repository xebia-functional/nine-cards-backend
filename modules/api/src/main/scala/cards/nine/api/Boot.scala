package cards.nine.api

import akka.actor.{ ActorSystem, Props }
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cards.nine.services.common.NineCardsConfig._
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val log = Logging(system, getClass)

  // create and start our service actor
  val service = system.actorOf(Props[NineCardsApiActor], "ninecards-server")

  implicit val timeout = Timeout(5.seconds)

  val host = defaultConfig.getString("http.host")
  val port = defaultConfig.getInt("http.port")

  IO(Http) ? Http.Bind(service, interface = host, port = port)

  log.info("Application started!")
}