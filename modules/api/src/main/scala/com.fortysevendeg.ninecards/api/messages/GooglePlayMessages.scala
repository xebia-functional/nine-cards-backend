package com.fortysevendeg.ninecards.api.messages

import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.AppGooglePlayInfo

object GooglePlayMessages {

  case class CategorizedApp(packageName: String, category: String)

  case class ApiGetAppsInfoRequest(items: List[String])

  case class ApiCategorizeAppsResponse(errors: List[String], items: List[CategorizedApp])

  case class ApiDetailAppsResponse(errors: List[String], items: List[AppGooglePlayInfo])
}
