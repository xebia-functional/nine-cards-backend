package com.fortysevendeg.ninecards.processes.messages

object GooglePlayAuthMessages {

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

}
