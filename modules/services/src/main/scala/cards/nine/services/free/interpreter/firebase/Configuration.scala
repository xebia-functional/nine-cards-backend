package cards.nine.services.free.interpreter.firebase

import cards.nine.services.common.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  sendNotificationPath: String,
  authorizationKey: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val base = "ninecards.backend.firebase"

    Configuration(
      protocol             = config.getString(s"$base.protocol"),
      host                 = config.getString(s"$base.host"),
      port                 = config.getOptionalInt(s"$base.port"),
      sendNotificationPath = config.getString(s"$base.paths.sendNotification"),
      authorizationKey     = config.getString(s"$base.authorizationKey")
    )
  }
}
