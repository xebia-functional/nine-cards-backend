package cards.nine.commons.config

import cards.nine.commons.config.ParserUtils.cache.CacheConnectionInfo
import cards.nine.commons.config.ParserUtils.database.PersistenceConnectionInfo
import cats.data.Validated.{ Invalid, Valid }
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

import scala.concurrent.duration.{ Duration, FiniteDuration }

object Domain {

  case class NineCardsConfiguration(
    db: DatabaseConfiguration,
    debugMode: Option[Boolean],
    editors: Map[String, String],
    google: GoogleConfiguration,
    http: HttpConfiguration,
    rankings: RankingsConfiguration,
    redis: RedisConfiguration,
    secretKey: String,
    salt: Option[String],
    loaderIO: LoaderIoConfiguration,
    test: TestConfiguration
  )

  object NineCardsConfiguration {
    def apply(config: NineCardsConfig): NineCardsConfiguration = {
      val prefix = "ninecards"

      NineCardsConfiguration(
        DatabaseConfiguration(config, prefix),
        config.getOptionalBoolean(s"$prefix.debugMode"),
        config.getMap(s"$prefix.editors"),
        GoogleConfiguration(config, prefix),
        HttpConfiguration(config, prefix),
        RankingsConfiguration(config, prefix),
        RedisConfiguration(config, prefix),
        config.getString(s"$prefix.secretKey"),
        config.getOptionalString(s"$prefix.salt"),
        LoaderIoConfiguration(config, prefix),
        TestConfiguration(config, prefix)
      )
    }
  }

  case class DatabaseConfiguration(
    default: DatabaseDefaultConfiguration,
    hikari: DatabaseHikariConfiguration
  )

  object DatabaseConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseConfiguration = {
      val prefix = s"$parentPrefix.db"

      DatabaseConfiguration(
        DatabaseDefaultConfiguration(config, prefix),
        DatabaseHikariConfiguration(config, prefix)
      )
    }
  }

  case class DatabaseDefaultConfiguration(
    driver: String,
    url: String,
    user: String,
    password: String
  )

  object DatabaseDefaultConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseDefaultConfiguration = {
      val prefix = s"$parentPrefix.default"

      ParserUtils.database.parseConnectionString(config.getString(s"$prefix.url")) match {
        case Valid(PersistenceConnectionInfo(user, password, url)) ⇒
          DatabaseDefaultConfiguration(
            driver   = config.getString(s"$prefix.driver"),
            url      = s"${config.getString(s"$prefix.urlPrefix")}$url",
            user     = user,
            password = password
          )
        case Invalid(errors) ⇒
          throw new RuntimeException(s"Database configuration not valid:\n${errors.toList.mkString("\n")}")
      }
    }
  }

  case class DatabaseHikariConfiguration(
    maximumPoolSize: Int,
    maxLifetime: Int
  )

  object DatabaseHikariConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): DatabaseHikariConfiguration = {
      val prefix = s"$parentPrefix.hikari"

      DatabaseHikariConfiguration(
        config.getInt(s"$prefix.maximumPoolSize"),
        config.getInt(s"$prefix.maxLifetime")
      )
    }
  }

  case class GoogleConfiguration(
    analytics: GoogleAnalyticsConfiguration,
    api: GoogleApiConfiguration,
    firebase: GoogleFirebaseConfiguration,
    play: GooglePlayConfiguration
  )

  object GoogleConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleConfiguration = {
      val prefix = s"$parentPrefix.google"

      GoogleConfiguration(
        GoogleAnalyticsConfiguration(config, prefix),
        GoogleApiConfiguration(config, prefix),
        GoogleFirebaseConfiguration(config, prefix),
        GooglePlayConfiguration(config, prefix)
      )
    }
  }

  case class GoogleAnalyticsConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    path: String,
    viewId: String
  )

  object GoogleAnalyticsConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleAnalyticsConfiguration = {
      val prefix = s"$parentPrefix.analytics"

      GoogleAnalyticsConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        config.getString(s"$prefix.path"),
        config.getString(s"$prefix.viewId")
      )
    }
  }

  case class GoogleApiConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    tokenInfo: GoogleApiTokenInfo
  )

  object GoogleApiConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleApiConfiguration = {
      val prefix = s"$parentPrefix.api"

      GoogleApiConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        GoogleApiTokenInfo(config, prefix)
      )
    }
  }

  case class GoogleApiTokenInfo(
    path: String,
    tokenIdQueryParameter: String
  )

  object GoogleApiTokenInfo {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleApiTokenInfo = {
      val prefix = s"$parentPrefix.tokenInfo"

      GoogleApiTokenInfo(
        config.getString(s"$prefix.path"),
        config.getString(s"$prefix.tokenIdQueryParameter")
      )
    }
  }

  case class GoogleFirebaseConfiguration(
    protocol: String,
    host: String,
    port: Option[Int],
    authorizationKey: String,
    paths: GoogleFirebasePaths
  )

  object GoogleFirebaseConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleFirebaseConfiguration = {
      val prefix = s"$parentPrefix.firebase"

      GoogleFirebaseConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getOptionalInt(s"$prefix.port"),
        config.getString(s"$prefix.authorizationKey"),
        GoogleFirebasePaths(config, prefix)
      )
    }
  }

  case class GoogleFirebasePaths(sendNotification: String)

  object GoogleFirebasePaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GoogleFirebasePaths = {
      val prefix = s"$parentPrefix.paths"

      GoogleFirebasePaths(
        config.getString(s"$prefix.sendNotification")
      )
    }
  }

  case class GooglePlayConfiguration(
    api: GooglePlayApiConfiguration,
    web: GooglePlayWebConfiguration,
    resolveInterval: FiniteDuration,
    resolveBatchSize: Int
  )

  object GooglePlayConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayConfiguration = {
      val prefix = s"$parentPrefix.play"

      GooglePlayConfiguration(
        GooglePlayApiConfiguration(config, prefix),
        GooglePlayWebConfiguration(config, prefix),
        convertToFiniteDuration(config.getString(s"$prefix.resolveInterval")),
        config.getInt(s"$prefix.resolveBatchSize")
      )
    }
  }

  case class GooglePlayApiConfiguration(
    protocol: String,
    host: String,
    port: Int,
    detailsBatchSize: Int,
    maxTotalConnections: Int,
    paths: GooglePlayApiPaths
  )

  object GooglePlayApiConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayApiConfiguration = {
      val prefix = s"$parentPrefix.api"

      GooglePlayApiConfiguration(
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port"),
        config.getInt(s"$prefix.detailsBatchSize"),
        config.getInt(s"$prefix.maxTotalConnections"),
        GooglePlayApiPaths(config, prefix)
      )
    }
  }

  case class GooglePlayApiPaths(
    bulkDetails: String,
    details: String,
    list: String,
    search: String,
    recommendations: String
  )

  object GooglePlayApiPaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayApiPaths = {
      val prefix = s"$parentPrefix.paths"

      GooglePlayApiPaths(
        config.getString(s"$prefix.bulkDetails"),
        config.getString(s"$prefix.details"),
        config.getString(s"$prefix.list"),
        config.getString(s"$prefix.search"),
        config.getString(s"$prefix.recommendations")
      )
    }
  }

  case class GooglePlayWebConfiguration(
    maxTotalConnections: Int,
    protocol: String,
    host: String,
    port: Int,
    paths: GooglePlayWebPaths
  )

  object GooglePlayWebConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayWebConfiguration = {
      val prefix = s"$parentPrefix.web"

      GooglePlayWebConfiguration(
        config.getInt(s"$prefix.maxTotalConnections"),
        config.getString(s"$prefix.protocol"),
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port"),
        GooglePlayWebPaths(config, prefix)
      )
    }
  }

  case class GooglePlayWebPaths(details: String)

  object GooglePlayWebPaths {
    def apply(config: NineCardsConfig, parentPrefix: String): GooglePlayWebPaths = {
      val prefix = s"$parentPrefix.paths"

      GooglePlayWebPaths(
        config.getString(s"$prefix.details")
      )
    }
  }

  case class HttpConfiguration(
    host: String,
    port: Int
  )

  object HttpConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): HttpConfiguration = {
      val prefix = s"$parentPrefix.http"

      HttpConfiguration(
        config.getString(s"$prefix.host"),
        config.getInt(s"$prefix.port")
      )
    }
  }

  case class RankingsConfiguration(
    oauth: RankingsOAuthConfiguration,
    actorInterval: FiniteDuration,
    rankingPeriod: Period,
    countriesPerRequest: Int,
    maxNumberOfAppsPerCategory: Int
  )

  object RankingsConfiguration {

    def apply(config: NineCardsConfig, parentPrefix: String): RankingsConfiguration = {
      val prefix = s"$parentPrefix.rankings"

      RankingsConfiguration(
        RankingsOAuthConfiguration(config, prefix),
        convertToFiniteDuration(config.getString(s"$prefix.actorInterval")),
        PeriodFormat.getDefault.parsePeriod(config.getString(s"$prefix.rankingPeriod")),
        config.getInt(s"$prefix.countriesPerRequest"),
        config.getInt(s"$prefix.maxNumberOfAppsPerCategory")
      )
    }

  }

  case class RankingsOAuthConfiguration(
    clientId: String,
    clientEmail: String,
    privateKey: String,
    privateKeyId: String,
    tokenUri: String,
    scopes: List[String]
  )

  object RankingsOAuthConfiguration {

    def apply(config: NineCardsConfig, parentPrefix: String): RankingsOAuthConfiguration = {
      val prefix = s"$parentPrefix.oauth"
      RankingsOAuthConfiguration(
        clientId     = config.getString(s"$prefix.clientId"),
        clientEmail  = config.getString(s"$prefix.clientEmail"),
        privateKey   = config.getString(s"$prefix.privateKey"),
        privateKeyId = config.getString(s"$prefix.privateKeyId"),
        tokenUri     = config.getString(s"$prefix.tokenUri"),
        scopes       = config.getStringList(s"$prefix.scopes")
      )
    }
  }

  case class RedisConfiguration(
    host: String,
    port: Int,
    secret: Option[String]
  )

  object RedisConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): RedisConfiguration = {
      val prefix = s"$parentPrefix.redis"

      ParserUtils.cache.parseConnectionString(config.getString(s"$prefix.url")) match {
        case Valid(CacheConnectionInfo(secret, host, port)) ⇒
          RedisConfiguration(
            host   = host,
            port   = port,
            secret = secret
          )
        case Invalid(errors) ⇒
          throw new RuntimeException(s"Cache configuration not valid:\n${errors.toList.mkString("\n")}")
      }
    }
  }

  case class TestConfiguration(
    androidId: String,
    token: String,
    localization: String,
    googlePlayDetailsUrl: String
  )

  object TestConfiguration {
    def apply(config: NineCardsConfig, parentPrefix: String): TestConfiguration = {
      val prefix = s"$parentPrefix.test"

      TestConfiguration(
        config.getString(s"$prefix.androidId"),
        config.getString(s"$prefix.token"),
        config.getString(s"$prefix.localization"),
        config.getString(s"$prefix.googlePlayDetailsAppUrl")

      )
    }
  }

  private[this] def convertToFiniteDuration(value: String) = {
    val duration = Duration(value)
    FiniteDuration(duration._1, duration._2)
  }

  case class LoaderIoConfiguration(
    verificationToken: String
  )

  object LoaderIoConfiguration {

    def apply(config: NineCardsConfig, parentPrefix: String): LoaderIoConfiguration =
      LoaderIoConfiguration(
        verificationToken = config.getString(s"$parentPrefix.loaderio.verificationToken")
      )

  }

}
