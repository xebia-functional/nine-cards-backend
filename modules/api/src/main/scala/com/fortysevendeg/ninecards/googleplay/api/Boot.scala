package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  implicit val system = ActorSystem("nine-cards-google-play-server-actor")

  val service = system.actorOf(Props[NineCardsGooglePlayActor], "nine-cards-google-play-server")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8081)
}
