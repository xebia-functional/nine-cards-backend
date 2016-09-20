package cards.nine.services.free.domain

case class TokenInfo(
  email_verified: String,
  email: String
)

case class WrongTokenInfo(error_description: String)