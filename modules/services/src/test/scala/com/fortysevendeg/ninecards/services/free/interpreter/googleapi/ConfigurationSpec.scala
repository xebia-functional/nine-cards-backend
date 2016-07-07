package com.fortysevendeg.ninecards.services.free.interpreter.googleapi

import com.fortysevendeg.ninecards.services.utils.DummyNineCardsConfig
import org.specs2.mutable.Specification

class ConfigurationSpec extends Specification with DummyNineCardsConfig {

  val configuration = Configuration.configuration

  "GoogleApi Configuration" should {
    "return the info related to the Google API stored in the config file" in {
      configuration.protocol shouldEqual googleApiProtocol
      configuration.port shouldEqual Option(googleApiPort)
      configuration.host shouldEqual googleApiHost
      configuration.tokenInfoUri shouldEqual googleApiTokenInfoUri
      configuration.tokenIdQueryParameter shouldEqual googleApiTokenIdParameterName
    }
  }

}
