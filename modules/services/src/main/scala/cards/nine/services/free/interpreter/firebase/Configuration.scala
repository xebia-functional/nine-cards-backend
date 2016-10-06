package cards.nine.services.free.interpreter.firebase

import cards.nine.commons.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  sendNotificationPath: String,
  authorizationKey: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val prefix = "ninecards.google.firebase"

    Configuration(
      protocol             = config.getString(s"$prefix.protocol"),
      host                 = config.getString(s"$prefix.host"),
      port                 = config.getOptionalInt(s"$prefix.port"),
      sendNotificationPath = config.getString(s"$prefix.paths.sendNotification"),
      authorizationKey     = config.getString(s"$prefix.authorizationKey")
    )
  }
}
