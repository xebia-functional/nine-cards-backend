package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import scala.concurrent.duration._
import spray.can.Http

object Boot extends App {

  implicit private val system = ActorSystem("nine-cards-google-play-server-actor")

  val actor = system.actorOf( Props[NineCardsGooglePlayActor], "nine-cards-google-play-server" )

  implicit val timeout = Timeout(5.seconds)

  val host = getConfigValue("ninecards.host")
  val port = getConfigNumber("ninecards.port")

  IO(Http) ! Http.Bind(actor, interface = host, port = port)

}
