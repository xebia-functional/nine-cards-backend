package cards.nine.processes.messages

object ApplicationMessages {

  case class GetAppsInfoResponse(errors: List[String], items: List[AppGooglePlayInfo])

  case class AppGooglePlayInfo(
    packageName: String,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )
}
