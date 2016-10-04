package cards.nine.processes.utils

import cards.nine.commons.NineCardsConfig

import scala.util.Random

trait DummyNineCardsConfig {

  val dbDefaultDriver = "org.h2.Driver"
  val dbDefaultUrl = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
  val dbDefaultUser = "sa"
  val dbDefaultPassword = ""

  val nineCardsSecretKey = "b91064c433a3d4723a622869273bf0d8"
  val nineCardsSalt = "ca349dde5a53d225eeb17074858465d5"

  val dbHikariMaximumPoolSize = 1
  val dbHikariMaxLifetime = 1

  def dummyConfigHocon(debugMode: Boolean) =
    s"""
       |db {
       |  default {
       |    driver = "$dbDefaultDriver"
       |    url = "$dbDefaultUrl"
       |    user = "$dbDefaultUser"
       |    password = "$dbDefaultPassword"
       |  }
       |  hikari {
       |    maximumPoolSize = $dbHikariMaximumPoolSize
       |    maxLifetime = $dbHikariMaxLifetime
       |  }
       |}
       |ninecards.backend {
       |  debugMode = $debugMode
       |}
       |ninecards.secretKey = "$nineCardsSecretKey"
       |ninecards.salt = "$nineCardsSalt"
     """.stripMargin

  def dummyConfig(debugMode: Boolean) = new NineCardsConfig(Option(dummyConfigHocon(debugMode)))

  implicit val config = dummyConfig(debugMode = false)

}