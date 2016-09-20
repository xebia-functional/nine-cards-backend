package cards.nine.services.utils

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.Header
import org.specs2.specification.BeforeAfterAll

trait MockServerService extends BeforeAfterAll {

  val jsonHeader = new Header("Content-Type", "application/json; charset=utf-8")
  val mockServerPort = 9999

  lazy val mockServer = startClientAndServer(mockServerPort)

  def beforeAll = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    mockServer
  }

  def afterAll = mockServer.stop

}
