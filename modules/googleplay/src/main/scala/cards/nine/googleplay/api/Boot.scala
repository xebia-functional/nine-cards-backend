package cards.nine.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.io.IO
import cards.nine.googleplay.config.NineCardsConfig._
import spray.can.Http

object Boot extends App {

  implicit private val system = ActorSystem("nine-cards-google-play-server-actor")
  val log = Logging(system, getClass)

  val actor = system.actorOf( Props[NineCardsGooglePlayActor], "nine-cards-google-play-server" )

  val host = getConfigValue("ninecards.googleplay.host")
  val port = getConfigNumber("ninecards.googleplay.port")

  IO(Http) ! Http.Bind(actor, interface = host, port = port)

  log.info("Application started!")
}
