package cards.nine.services.utils

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.Header
import org.specs2.specification.BeforeAfterAll

trait MockServerService extends BeforeAfterAll {

  val jsonHeader = new Header("Content-Type", "application/json; charset=utf-8")
  val mockServerPort = 9999

  lazy val mockServer = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    startClientAndServer(mockServerPort)
  }

  def beforeAll = {}
  def afterAll = mockServer.stop

}
