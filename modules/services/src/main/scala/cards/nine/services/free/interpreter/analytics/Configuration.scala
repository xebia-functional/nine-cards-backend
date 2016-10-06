package cards.nine.services.free.interpreter.analytics

import cards.nine.commons.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  uri: String,
  viewId: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val prefix = "ninecards.google.analytics"

    Configuration(
      protocol = config.getString(s"$prefix.protocol"),
      host     = config.getString(s"$prefix.host"),
      port     = config.getOptionalInt(s"$prefix.port"),
      uri      = config.getString(s"$prefix.uri"),
      viewId   = config.getString(s"$prefix.viewId")
    )
  }
}
