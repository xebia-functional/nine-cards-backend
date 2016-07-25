package com.fortysevendeg.ninecards.api.messages

import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.CategorizedApp

object GooglePlayMessages {

  case class ApiCategorizeAppsRequest(items: List[String])

  case class ApiCategorizeAppsResponse(errors: List[String], items: List[CategorizedApp])
}
