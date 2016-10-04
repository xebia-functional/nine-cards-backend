package cards.nine.services.free.interpreter.googleplay

import cards.nine.commons.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  recommendationsPath: String,
  resolveOnePath: String,
  resolveManyPath: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration =
    Configuration(
      protocol            = config.getString("googleplay.protocol"),
      host                = config.getString("googleplay.host"),
      port                = config.getOptionalInt("googleplay.port"),
      recommendationsPath = config.getString("googleplay.paths.recommendations"),
      resolveOnePath      = config.getString("googleplay.paths.resolveOne"),
      resolveManyPath     = config.getString("googleplay.paths.resolveMany")
    )
}
