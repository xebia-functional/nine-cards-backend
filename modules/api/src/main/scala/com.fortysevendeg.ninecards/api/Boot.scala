package com.fortysevendeg.ninecards.api

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.fortysevendeg.ninecards.services.common.NineCardsConfig._
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor
  val service = system.actorOf(Props[NineCardsApiActor], "ninecards-server")

  implicit val timeout = Timeout(5.seconds)

  val host = defaultConfig.getString("http.host")
  val port = defaultConfig.getInt("http.port")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = host, port = port)
}