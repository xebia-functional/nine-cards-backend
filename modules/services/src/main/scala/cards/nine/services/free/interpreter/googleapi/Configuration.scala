package cards.nine.services.free.interpreter.googleapi

import cards.nine.commons.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  tokenInfoUri: String,
  tokenIdQueryParameter: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val prefix = "ninecards.google.api"

    Configuration(
      protocol              = config.getString(s"$prefix.protocol"),
      host                  = config.getString(s"$prefix.host"),
      port                  = config.getOptionalInt(s"$prefix.port"),
      tokenInfoUri          = config.getString(s"$prefix.tokenInfo.path"),
      tokenIdQueryParameter = config.getString(s"$prefix.tokenInfo.tokenIdQueryParameter")
    )
  }
}
