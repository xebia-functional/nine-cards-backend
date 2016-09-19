package cards.nine.services.free.interpreter.googleapi

import cards.nine.services.utils.DummyNineCardsConfig
import org.specs2.mutable.Specification

class ConfigurationSpec extends Specification with DummyNineCardsConfig {

  val configuration = Configuration.configuration

  "GoogleApi Configuration" should {
    "return the info related to the Google API stored in the config file" in {
      configuration.protocol shouldEqual googleapi.protocol
      configuration.port shouldEqual Option(googleapi.port)
      configuration.host shouldEqual googleapi.host
      configuration.tokenInfoUri shouldEqual googleapi.tokenInfoUri
      configuration.tokenIdQueryParameter shouldEqual googleapi.tokenIdParameterName
    }
  }

}
