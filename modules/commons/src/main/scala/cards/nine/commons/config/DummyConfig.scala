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
      val driver = "org.h2.Driver"
      val url = s"jdbc:h2:mem:test-${Random.nextFloat()};DB_CLOSE_DELAY=-1"
      val user = "sa"
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
  }

  object test {
    val androidId = "65H9fv28456fj939"
    val token = "kfh4343JGi39034LS98fi34"
    val localization = "en-US"
    val googlePlayDetailsAppUrl = "https://play.google.com/store/apps/details"
    val privateKey = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDKO0tlnUd/v+9f\n82sscScqRqpNSpv+g7SX92OWRWMQMp6v/QuBhnfJI0h+R4iKB0v1p43JQwD9zWTV\no6NAm0BVKnAURYmd4Hz8fE6gnogSXuywxxyChBGm3numFt4cD+ITwmL47paOl7fE\nB7b+gDXLfoPBEspcY6Aq/sd011Mj+UlqunSkVNqgwho2Ib0XE1n123y4yjqhOYDg\nRriQYthLWR8haY+xCBKqaY2vsxoUo/x3LDHY9qamzZK91M8pr78WSEQtvgdL4T+S\n7eMO4vZlIz4pqHmqVSp94i0xZqbjwU9Fl8tnZSftSI3j8a7lh3VZQutOyqdmAXCs\nOzsqm1x/AgMBAAECggEAZeBfCC6mUTXoqgafbAEIwORecJdwZFQHCzNGSPJliEhn\n5rJsNHEAMSSEJZF1uEsaugMRi10kx7bL4FASMr7DNuQtGBk7zXvEmxEoLwQN+ms1\ng4ya0SklF1InKjGE7NAXjOq1H0BaOjU2Dxvv1N/TudLfHsmPheFuU5qL8lFzAcBy\nv50Yzc6D7rvah/tZzq8RUDcbdeQoIaBO3KLQFKFgepWyYP3sumryDIvPS3/VBJIF\nPXLIhzmmGQkxowG94oPPlVAmnz7rJJ4gXvpcp6yYVY7KyymCPexiybryGmRCCbbO\nsY9z6QEysaqXuPvlpf4fnDpFFKt7VnEDco6c0OmpIQKBgQDyyZ3lmwLtXVMROLzX\nrBIDRgc3Oh9/Mi2SqrqA2k5Xegbu9yQ+kYQDC5EZKDa+ehdmTLjQyQRZoMe55Cmc\nndFHwtTgfBczslbpaA4qp3f0y9vGvtcXYU0SEZ2ABQ2+7rjoTBlozzGrZ61ZiwA4\n8jnuAQdDHSbmxnybdJG1AZR8DwKBgQDVPKyuF5XC4rYbUF0HBhZUMBXvbr45xa/c\n5RIpzgdf7trYh//XvwzctB2xPbZ2cfLbMk4s5pXC8xfK4DvFxz5/0NH/Qqnq25ZZ\nAyzcSBzqRfvycYHe6azZFEUE2H3/hZ22Ci/hJRVpKTzfmVP6k5irJHPRNJ7gBRYj\nDFUEfDVokQKBgFtqFo2xQ/TjwrclSdVa8J1S7Lyaoelel1I1/mstx3mgdKPdYkQ8\nvhv3avax8YrepZjbImtahMzKgOk5Q4G42mfOsCCmGIZai+buSwc8byo459N5X7tp\n437+KvNc88QEVpMAtECGYwlKzDZ+4+KNMcqvkHBwtYkHCzvtND+XDLV5AoGBANJJ\nManFzIUSerBZAkTI9gA0I42p0qK6l4HpzY24hoFO1jcvd1jWKeMFJTsDNwt7uBn+\ndCXHh+1nOhFyLMAQi0wVLOLkZRkdUBmcDgN2gj1uotYmpgKkwzaYzK/IqAjzReKe\nDTWlEoZQip2fYbf9ElPEcQrhL8SQf5I0uSrhKvJhAoGABaJ5mnrbLvJvp/qp8daI\nDBEmhFwzy3L3h76FyONBsDkMvkXc3ThfLB6EbDlP6TxyNsQEgL/kGG+lym6ZpUOE\nH0EJRER90IjoZzn9YjJQNzgTb0/YniL+Mgq8ndvVSkBmGROXzwHxuRb3qE1exyoj\nGToA5bURJoBWwKprbykGdVc=\n-----END PRIVATE KEY-----\n"
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
