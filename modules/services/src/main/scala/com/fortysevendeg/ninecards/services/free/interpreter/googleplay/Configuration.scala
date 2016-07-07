package com.fortysevendeg.ninecards.services.free.interpreter.googleplay

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  resolveOneUri: String,
  resolveManyUri: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration =
    Configuration(
      protocol       = config.getString("googleplay.protocol"),
      host           = config.getString("googleplay.host"),
      port           = config.getOptionalInt("googleplay.port"),
      resolveOneUri  = config.getString("googleplay.uri.resolveOne"),
      resolveManyUri = config.getString("googleplay.uri.resolveMany")
    )
}
