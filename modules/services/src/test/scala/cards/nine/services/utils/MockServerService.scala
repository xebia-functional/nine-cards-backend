package cards.nine.services.utils

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.Header
import org.specs2.specification.AfterAll

trait MockServerService extends AfterAll {

  val jsonHeader = new Header("Content-Type", "application/json; charset=utf-8")
  val mockServerPort = 9999

  lazy val mockServer = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    startClientAndServer(mockServerPort)
  }

  def afterAll = mockServer.stop

}
