package cards.nine.services.free.interpreter.analytics

import cards.nine.services.common.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  uri: String,
  viewId: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val base = "ninecards.backend.googleanalytics"
    Configuration(
      protocol = config.getString(s"${base}.protocol"),
      host     = config.getString(s"${base}.host"),
      port     = config.getOptionalInt(s"${base}.port"),
      uri      = config.getString(s"${base}.uri"),
      viewId   = config.getString(s"${base}.viewId")
    )
  }
}
