package com.fortysevendeg.ninecards.googleplay.service.util

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.specs2.specification.{BeforeAfterEach, BeforeAfterAll}

trait MockServer extends BeforeAfterAll with BeforeAfterEach {

  protected[this] def mockServerPort: Int

  protected[this] lazy val mockServer = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    startClientAndServer(mockServerPort)
  }

  override def beforeAll = {}
  override def afterAll = mockServer.stop

  override def before = mockServer.reset()
  override def after = {}
}
