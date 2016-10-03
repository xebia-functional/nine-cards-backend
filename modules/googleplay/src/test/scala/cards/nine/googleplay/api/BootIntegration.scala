package cards.nine.googleplay.api

import java.net.Socket
import org.specs2.matcher.EventuallyMatchers
import org.specs2.mutable.Specification
import scala.concurrent.duration._

class BootIntegration extends Specification with EventuallyMatchers {

  "Starting the app" should {
    "allow a connection to be made" in {

      Boot.main(Array())

      eventually(retries = 100, sleep = 100.millis){
        val socket = new Socket("localhost", 8081)
        socket.isConnected
      }
    }
  }
}
