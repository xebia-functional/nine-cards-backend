package com.fortysevendeg.ninecards.services.utils

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

import scala.util.Random

trait DummyNineCardsConfig {

  val dbDefaultDriver = "org.h2.Driver"
  val dbDefaultUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
  val dbDefaultUser = "sa"
  val dbDefaultPassword = ""

  val googleApiProtocol = "http"
  val googleApiPort = 8080
  val googleApiHost = "localhost"
  val googleApiTokenInfoUri = "/oauth2/v3/tokeninfo"
  val googleApiTokenIdParameterName = "id_token"

  val dummyConfigJson =
    s"""
       |db {
       |  default {
       |    driver = "$dbDefaultDriver"
       |    url = "$dbDefaultUrl"
       |    user = "$dbDefaultUser"
       |    password = "$dbDefaultPassword"
       |  }
       |}
       |googleapi {
       |  protocol = "$googleApiProtocol"
       |  port = $googleApiPort
       |  host = "$googleApiHost"
       |  tokenInfo {
       |    uri = "$googleApiTokenInfoUri"
       |    tokenIdQueryParameter = "$googleApiTokenIdParameterName"
       |  }
       |}
     """.stripMargin

  implicit val dummyConfig = new NineCardsConfig(Option(dummyConfigJson))

}
