package cards.nine.processes.messages

object GooglePlayAuthMessages {

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

}
