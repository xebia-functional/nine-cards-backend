package com.fortysevendeg.ninecards.services.free.interpreter.analytics

import com.fortysevendeg.ninecards.services.utils.DummyNineCardsConfig
import org.specs2.mutable.Specification

class ConfigurationSpec extends Specification with DummyNineCardsConfig {

  "GoogleAnalytics Configuration" should {
    "return the info related to the Google Analytics API stored in the config file" in {
      val conf = Configuration.configuration
      conf.protocol shouldEqual googleanalytics.protocol
      conf.host shouldEqual googleanalytics.host
      conf.port shouldEqual Option(googleanalytics.port)
      conf.uri shouldEqual googleanalytics.uri
      conf.viewId shouldEqual googleanalytics.viewId
    }
  }

}
