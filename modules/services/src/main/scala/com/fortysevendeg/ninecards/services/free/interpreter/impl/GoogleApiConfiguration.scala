package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

case class GoogleApiConfiguration(
  protocol: String,
  host: String,
  port: Option[Int],
  tokenInfoUri: String,
  tokenIdQueryParameter: String
)

object GoogleApiConfiguration {
  implicit def googleApiConfiguration(implicit config: NineCardsConfig): GoogleApiConfiguration =
    GoogleApiConfiguration(
      protocol              = config.getString("googleapi.protocol"),
      host                  = config.getString("googleapi.host"),
      port                  = config.getOptionalInt("googleapi.port"),
      tokenInfoUri          = config.getString("googleapi.tokenInfo.uri"),
      tokenIdQueryParameter = config.getString("googleapi.tokenInfo.tokenIdQueryParameter")
    )
}
