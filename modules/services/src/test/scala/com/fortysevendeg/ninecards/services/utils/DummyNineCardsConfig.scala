package com.fortysevendeg.ninecards.services.utils

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

import scala.util.Random

trait DummyNineCardsConfig {

  object db {
    object default {
      val driver = "org.h2.Driver"
      val url = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
      val user = "sa"
      val password = ""
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

  object googleplay {
    val protocol = "http"
    val host = "localhost"
    val port = 8081
    object paths {
      val recommendations = "/googleplay/recommendations"
      val resolveOne = "/googleplay/cards"
      val resolveMany = "/googleplay/cards"
    }
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
       |db {
       |  default {
       |    driver = "${db.default.driver}"
       |    url = "${db.default.url}"
       |    user = "${db.default.user}"
       |    password = "${db.default.password}"
       |  }
       |}
       |googleapi {
       |  host = "${googleapi.host}"
       |  port = ${googleapi.port}
       |  protocol = "${googleapi.protocol}"
       |  tokenInfo {
       |    uri = "${googleapi.tokenInfoUri}"
       |    tokenIdQueryParameter = "${googleapi.tokenIdParameterName}"
       |  }
       |}
       |googleplay {
       |  host = "${googleplay.host}"
       |  port = ${googleplay.port}
       |  protocol = "${googleplay.protocol}"
       |  paths = {
       |    recommendations = ${googleplay.paths.recommendations}
       |    resolveOne = ${googleplay.paths.resolveOne}
       |    resolveMany = ${googleplay.paths.resolveMany}
       |  }
       |}
       |ninecards.backend {
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
       |}

     """.stripMargin

  implicit val dummyConfig = new NineCardsConfig(Option(dummyConfigHocon))

}
