package cards.nine.services.utils

import cards.nine.commons.NineCardsConfig

import scala.util.Random

trait DummyNineCardsConfig {

  object db {
    object default {
      val driver = "org.h2.Driver"
      val url = "jdbc:h2:mem:"
      val user = "sa"
      val password = ""
    }
    object domain {
      val driver = "org.h2.Driver"
      val url = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
      val user = "sa"
      val password = ""
    }
    object hikari {
      val maximumPoolSize = 1
    }
  }

  object firebase {
    val authorizationKey = "1a2b3cb4d5e"
    val protocol = "http"
    val port = 8080
    val host = "localhost"
    object paths {
      val sendNotification = "/fcm/send"
    }
  }

  object googleapi {
    val protocol = "http"
    val port = 8080
    val host = "localhost"
    val tokenInfoUri = "/oauth2/v3/tokeninfo"
    val tokenIdParameterName = "id_token"
  }

  object googleanalytics {
    val protocol = "http"
    val host = "localhost"
    val port = 8090
    val uri = "/v4/reports:batchGet"
    val viewId = "12345"
    object default {
      val dateRangeLength = 21
      val rankingLength = 16
    }
  }

  val dummyConfigHocon =
    s"""
       |ninecards {
       |  db {
       |    default {
       |      driver = "${db.default.driver}"
       |      url = "${db.default.url}"
       |      user = "${db.default.user}"
       |      password = "${db.default.password}"
       |    }
       |    domain {
       |      driver = "${db.domain.driver}"
       |      url = "${db.domain.url}"
       |      user = "${db.domain.user}"
       |      password = "${db.domain.password}"
       |    }
       |    hikari {
       |      maximumPoolSize = ${db.hikari.maximumPoolSize}
       |    }
       |  }
       |  firebase {
       |    authorizationKey = "${firebase.authorizationKey}"
       |    protocol = "${firebase.protocol}"
       |    host = "${firebase.host}"
       |    port = ${firebase.port}
       |    paths {
       |      sendNotification = "${firebase.paths.sendNotification}"
       |    }
       |  }
       |  googleanalytics {
       |    host = "${googleanalytics.host}"
       |    port = ${googleanalytics.port}
       |    protocol = "${googleanalytics.protocol}"
       |    uri = "${googleanalytics.uri}"
       |    viewId = "${googleanalytics.viewId}"
       |  }
       |  googleapi {
       |    host = "${googleapi.host}"
       |    port = ${googleapi.port}
       |    protocol = "${googleapi.protocol}"
       |    tokenInfo {
       |      uri = "${googleapi.tokenInfoUri}"
       |      tokenIdQueryParameter = "${googleapi.tokenIdParameterName}"
       |    }
       |  }
       |}

     """.stripMargin

  implicit val dummyConfig = new NineCardsConfig(Option(dummyConfigHocon))

}
