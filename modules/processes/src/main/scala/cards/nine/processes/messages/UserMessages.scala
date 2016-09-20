package cards.nine.processes.messages

object UserMessages {

  case class LoginRequest(
    email: String,
    androidId: String,
    sessionToken: String,
    tokenId: String
  )

  case class LoginResponse(
    apiKey: String,
    sessionToken: String
  )

}
