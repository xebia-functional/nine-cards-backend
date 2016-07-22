package com.fortysevendeg.ninecards.processes.messages

object GooglePlayMessages {

  case class CategorizeAppsResponse(errors: List[String], items: List[CategorizedApp])

  case class CategorizedApp(packageName: String, category: String)

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )
}
