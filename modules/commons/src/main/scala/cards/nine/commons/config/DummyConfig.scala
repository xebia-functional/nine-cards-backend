package cards.nine.commons.config

import cards.nine.commons.config.Domain.NineCardsConfiguration

import scala.util.Random

trait DummyConfig {

  object common {
    val protocol = "http"
    val port = 8080
    val host = "localhost"
  }

  object db {

    object default {
      val driver = "org.h2.Driver"
      val url = "jdbc:h2:mem:"
      val user = "sa"
      val password = ""
    }

    object domain {
      val driver = "org.postgresql.Driver"
      val url = "jdbc:postgresql://localhost/ninecards_travis_ci_test"
      val user = "postgres"
      val password = ""
    }

    object hikari {
      val maximumPoolSize = 1
      val maxLifetime = 1
    }

  }

  object firebase {
    val authorizationKey = "1a2b3cb4d5e"

    object paths {
      val sendNotification = "/fcm/send"
    }

  }

  object googleanalytics {
    val uri = "/v4/reports:batchGet"
    val viewId = "12345"
  }

  object googleapi {
    val tokenInfoUri = "/oauth2/v3/tokeninfo"
    val tokenIdParameterName = "id_token"
  }

  object googleplay {

    object api {

      object paths {
        val bulkDetails = "/fdfe/bulkDetails"
        val details = "/fdfe/details"
        val list = "/fdfe/list"
        val search = "/fdfe/search"
        val recommendations = "/fdfe/rec"
      }

    }

    object web {

      object paths {
        val details = "/store/apps/details"
      }

    }

    val resolveInterval = "1 second"
    val resolveBatchSize = 1
  }

  object ninecards {
    val salt = "0987654321"
    val secretKey = "1234567890"
  }

  object rankings {
    val actorInterval = "1 hour"
    val rankingPeriod = "30 days"
    val countriesPerRequest = 2
    val maxNumberOfAppsPerCategory = 100

    object oauth {
      val clientId = "12345"
      val clientEmail = "client@ema.il"
      val privateKey = """----BEGIN PRIVATE KEY----\nBASE64+=TEXT\n-----END PRIVATE KEY -----"""
      val privateKeyId = "abcdef0123456789"
      val tokenUri = ""
      val scopes = List("http://www.nine.cards/auth/testing.only")
    }
  }

  object test {
    val androidId = "65H9fv28456fj939"
    val token = "kfh4343JGi39034LS98fi34"
    val localization = "en-US"
    val googlePlayDetailsAppUrl = "https://play.google.com/store/apps/details"
  }

  def dummyConfigHocon(debugMode: Boolean) =
    s"""
       |ninecards {
       |  db {
       |    default {
       |      driver = "${db.default.driver}"
       |      url = "${db.default.url}"
       |      user = "${db.default.user}"
       |      password = "${db.default.password}"
       |    }
       |    hikari {
       |      maximumPoolSize = ${db.hikari.maximumPoolSize}
       |      maxLifetime = ${db.hikari.maxLifetime}
       |    }
       |  }
       |  debugMode = $debugMode
       |  google {
       |    analytics {
       |      host = "${common.host}"
       |      port = ${common.port}
       |      protocol = "${common.protocol}"
       |      path = "${googleanalytics.uri}"
       |      viewId = "${googleanalytics.viewId}"
       |    }
       |    api {
       |      host = "${common.host}"
       |      port = ${common.port}
       |      protocol = "${common.protocol}"
       |      tokenInfo {
       |        path = "${googleapi.tokenInfoUri}"
       |        tokenIdQueryParameter = "${googleapi.tokenIdParameterName}"
       |      }
       |    }
       |    firebase {
       |      authorizationKey = "${firebase.authorizationKey}"
       |      protocol = "${common.protocol}"
       |      host = "${common.host}"
       |      port = ${common.port}
       |      paths {
       |        sendNotification = "${firebase.paths.sendNotification}"
       |      }
       |    }
       |    play {
       |      api {
       |        protocol = "${common.protocol}"
       |        host = "${common.host}"
       |        port = ${common.port}
       |        paths {
       |          bulkDetails = "${googleplay.api.paths.bulkDetails}"
       |          details = "${googleplay.api.paths.details}"
       |          list = "${googleplay.api.paths.list}"
       |          search = "${googleplay.api.paths.search}"
       |          recommendations = "${googleplay.api.paths.recommendations}"
       |        }
       |      }
       |      web {
       |        protocol = "${common.protocol}"
       |        host = "${common.host}"
       |        port = ${common.port}
       |        paths {
       |          details = "${googleplay.web.paths.details}"
       |        }
       |      }
       |      resolveInterval = ${googleplay.resolveInterval}
       |      resolveBatchSize = ${googleplay.resolveBatchSize}
       |    }
       |  }
       |    http {
       |    host = "${common.host}"
       |    port = ${common.port}
       |  }
       |  rankings {
       |    actorInterval = ${rankings.actorInterval}
       |    rankingPeriod = ${rankings.rankingPeriod}
       |    countriesPerRequest = ${rankings.countriesPerRequest}
       |    maxNumberOfAppsPerCategory = ${rankings.maxNumberOfAppsPerCategory}
       |    oauth {
       |      clientId = "${rankings.oauth.clientId}"
       |      clientEmail = "${rankings.oauth.clientEmail}"
       |      privateKey = "${rankings.oauth.privateKey}"
       |      privateKeyId = "${rankings.oauth.privateKeyId}"
       |      tokenUri = "${rankings.oauth.tokenUri}"
       |      scopes = [ "${rankings.oauth.scopes.head}" ]
       |    }
       |  }
       |  redis {
       |    host = "${common.host}"
       |    port = ${common.port}
       |  }
       |  salt = "${ninecards.salt}"
       |  secretKey = "${ninecards.secretKey}"
       |  test {
       |    androidId = "${test.androidId}"
       |    token = "${test.token}"
       |    localization = "${test.localization}"
       |    googlePlayDetailsAppUrl = "${test.googlePlayDetailsAppUrl}"
       |  }
       |}

     """.stripMargin

  def dummyConfig(debugMode: Boolean) = new NineCardsConfig(Option(dummyConfigHocon(debugMode)))

  implicit val config: NineCardsConfiguration = NineCardsConfiguration(dummyConfig(debugMode = false))

  val debugConfig: NineCardsConfiguration = NineCardsConfiguration(dummyConfig(debugMode = true))
}
