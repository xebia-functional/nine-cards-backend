package com.fortysevendeg.ninecards.services.free.interpreter.firebase

import com.fortysevendeg.ninecards.services.utils.DummyNineCardsConfig
import org.specs2.mutable.Specification

class ConfigurationSpec extends Specification with DummyNineCardsConfig {

  val configuration = Configuration.configuration

  "Firebase Configuration" should {
    "return the info related to Firebase stored in the config file" in {
      configuration.authorizationKey shouldEqual firebase.authorizationKey
      configuration.protocol shouldEqual firebase.protocol
      configuration.host shouldEqual firebase.host
      configuration.port shouldEqual Option(firebase.port)
      configuration.sendNotificationUri shouldEqual firebase.uri.sendNotification
    }
  }

}
