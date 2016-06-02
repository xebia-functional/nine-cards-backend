package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
import scala.concurrent.duration._

object Boot extends App {

  implicit val system = ActorSystem("nine-cards-google-play-server-actor")

  val service = system.actorOf(Props[NineCardsGooglePlayActor], "nine-cards-google-play-server")

  implicit val timeout = Timeout(5.seconds)

  val host = getConfigValue("ninecards.host")
  val port = getConfigValue("ninecards.port").toInt

  IO(Http) ? Http.Bind(service, interface = host, port = port)
}
