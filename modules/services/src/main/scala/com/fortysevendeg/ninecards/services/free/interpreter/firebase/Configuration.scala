package com.fortysevendeg.ninecards.services.free.interpreter.firebase

import com.fortysevendeg.ninecards.services.common.NineCardsConfig

case class Configuration(
  protocol: String,
  host: String,
  port: Option[Int],
  sendNotificationUri: String,
  authorizationKey: String
)

object Configuration {
  implicit def configuration(implicit config: NineCardsConfig): Configuration = {
    val base = "firebase"

    Configuration(
      protocol            = config.getString(s"$base.protocol"),
      host                = config.getString(s"$base.host"),
      port                = config.getOptionalInt(s"$base.port"),
      sendNotificationUri = config.getString(s"$base.uri.sendNotification"),
      authorizationKey    = config.getString(s"$base.authorizationKey")
    )
  }
}
