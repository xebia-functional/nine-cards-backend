package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.common.NineCardsConfig._

case class GoogleApiConfiguration(
  protocol: String,
  host: String,
  port: Option[Int],
  tokenInfoUri: String,
  tokenIdQueryParameter: String)

object GoogleApiConfiguration {
  implicit def googleApiConfiguration: GoogleApiConfiguration = GoogleApiConfiguration(
    protocol = getString("googleapi.protocol"),
    host = getString("googleapi.host"),
    port = getOptionalInt("googleapi.port"),
    tokenInfoUri = getString("googleapi.tokenInfo.uri"),
    tokenIdQueryParameter = getString("googleapi.tokenInfo.tokenIdQueryParameter")
  )
}
