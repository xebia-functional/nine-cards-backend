package com.fortysevendeg.ninecards.services.common

import com.fortysevendeg.ninecards.services.free.interpreter.impl.GoogleApiConfiguration
import com.fortysevendeg.ninecards.services.utils.DummyNineCardsConfig
import org.specs2.mutable.Specification

class GoogleApiConfigurationSpec extends Specification with DummyNineCardsConfig {

  val googleApiConfiguration = GoogleApiConfiguration.googleApiConfiguration

  "GoogleApiConfiguration" should {
    "return the info related to the Google API stored in the config file" in {
      googleApiConfiguration.protocol shouldEqual googleApiProtocol
      googleApiConfiguration.port shouldEqual Option(googleApiPort)
      googleApiConfiguration.host shouldEqual googleApiHost
      googleApiConfiguration.tokenInfoUri shouldEqual googleApiTokenInfoUri
      googleApiConfiguration.tokenIdQueryParameter shouldEqual googleApiTokenIdParameterName
    }
  }

}
