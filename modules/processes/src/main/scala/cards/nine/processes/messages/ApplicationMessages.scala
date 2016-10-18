package cards.nine.processes.messages

import cards.nine.domain.application.Package

object ApplicationMessages {

  case class GetAppsInfoResponse(errors: List[Package], items: List[AppGooglePlayInfo])

  case class AppGooglePlayInfo(
    packageName: Package,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )
}
