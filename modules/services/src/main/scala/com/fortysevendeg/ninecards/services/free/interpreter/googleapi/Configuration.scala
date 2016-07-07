package com.fortysevendeg.ninecards.services.free.interpreter.googleapi

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  tokenInfoUri: String,
  tokenIdQueryParameter: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration =
    Configuration(
      protocol              = config.getString("googleapi.protocol"),
      host                  = config.getString("googleapi.host"),
      port                  = config.getOptionalInt("googleapi.port"),
      tokenInfoUri          = config.getString("googleapi.tokenInfo.uri"),
      tokenIdQueryParameter = config.getString("googleapi.tokenInfo.tokenIdQueryParameter")
    )
}
