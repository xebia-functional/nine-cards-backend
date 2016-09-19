package com.fortysevendeg.ninecards.googleplay.service.util

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.specs2.specification.BeforeAfterAll

trait MockServer extends BeforeAfterAll {

  protected[this] def mockServerPort: Int

  protected[this] lazy val mockServer = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    startClientAndServer(mockServerPort)
  }

  def beforeAll = {}

  def afterAll = mockServer.stop
}
