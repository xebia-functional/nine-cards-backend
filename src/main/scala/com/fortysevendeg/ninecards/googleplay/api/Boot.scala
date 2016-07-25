package com.fortysevendeg.ninecards.googleplay.api

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cats.~>
import com.fortysevendeg.ninecards.config.NineCardsConfig._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import scala.concurrent.duration._
import scalaz.concurrent.Task
import spray.can.Http

object Boot extends App {

  implicit private val system = ActorSystem("nine-cards-google-play-server-actor")

  private val interpreter: GooglePlay.Ops ~> Task = Wiring.interpreter()

  private val service = system.actorOf(
    Props( classOf[NineCardsGooglePlayActor], interpreter),
    "nine-cards-google-play-server"
  )

  implicit private val timeout = Timeout(5.seconds)

  private val host = getConfigValue("ninecards.host")
  private val port = getConfigNumber("ninecards.port")

  IO(Http) ? Http.Bind(service, interface = host, port = port)
}
