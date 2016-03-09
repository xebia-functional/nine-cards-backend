package com.fortysevendeg.ninecards.googleplay.api

import java.net.Socket
import org.specs2.matcher.EventuallyMatchers
import org.specs2.mutable.Specification

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class BootTest extends Specification with EventuallyMatchers {

  "Starting the app" should {
    "allow a connection to be made" in {

      Boot.main(Array())

      eventually{
        val socket = new Socket("localhost", 8081)
        socket.isConnected
      }
    }
  }
}
