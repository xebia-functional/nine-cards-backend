package com.fortysevendeg.ninecards.processes.utils

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

import scala.util.Random

trait DummyNineCardsConfig {

  val dbDefaultDriver = "org.h2.Driver"
  val dbDefaultUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
  val dbDefaultUser = "sa"
  val dbDefaultPassword = ""

  val nineCardsSecretKey = "b91064c433a3d4723a622869273bf0d8"
  val nineCardsSalt = "ca349dde5a53d225eeb17074858465d5"

  val dummyConfigHocon =
    s"""
       |db {
       |  default {
       |    driver = "$dbDefaultDriver"
       |    url = "$dbDefaultUrl"
       |    user = "$dbDefaultUser"
       |    password = "$dbDefaultPassword"
       |  }
       |}
       |ninecards.secretKey = "$nineCardsSecretKey"
       |ninecards.salt = "$nineCardsSalt"
     """.stripMargin

  implicit val dummyConfig = new NineCardsConfig(Option(dummyConfigHocon))

}