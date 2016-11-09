package cards.nine.domain.oauth

/**
  * In the Google OAuth Service, a Service Account is used to grant a server application
  * access to data that belongs not to a user, but to the application itself.
  * http://developers.google.com/identity/protocols/OAuth2ServiceAccount
  */
case class ServiceAccount(
  clientId: String,
  clientEmail: String,
  privateKey: String,
  privateKeyId: String,
  tokenUri: String,
  scopes: List[String]
)

/**
  * An AccessToken object contains a String whose value is used
  *  as an OAuth 2.0 Access Token, as defined in
  *  https://tools.ietf.org/html/rfc6749#section-1.4
  */
case class AccessToken(value: String) extends AnyVal
