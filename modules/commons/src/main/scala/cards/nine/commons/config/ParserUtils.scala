package cards.nine.commons.config

import cats.data.{ Validated, ValidatedNel }
import cats.implicits._

object ParserUtils {

  object database {
    val protocol = "([^:\\/]+:\\/\\/)"

    val userInfo = "([^:]+)?(:([^@]+))?@"

    val serverInstanceInfo = s"([^\\/:]+)(:\\d+)?(\\/.+)?"

    val DatabaseUrl = s"$protocol($userInfo)?($serverInstanceInfo)?".r

    case class PersistenceConnectionInfo(user: String, password: String, url: String)

    def parseConnectionString(raw: String): ValidatedNel[ConfigError, PersistenceConnectionInfo] =
      raw match {
        case DatabaseUrl(_, _, u, _, p, ur, _, _, _) ⇒

          val user = Option(u).toValidNel[ConfigError](MissingConfigValue("PostgreSQL user missing"))
          val password: ValidatedNel[ConfigError, String] = Validated.valid(Option(p).getOrElse(""))
          val url = Option(ur).toValidNel[ConfigError](MissingConfigValue("PostgreSQL URL missing"))

          (user |@| password |@| url) map { (userValue, passwordValue, urlValue) ⇒
            PersistenceConnectionInfo(
              user     = userValue,
              password = passwordValue,
              url      = urlValue
            )
          }
        case _ ⇒
          Validated.invalidNel(
            UnexpectedConnectionURL("Unexpected database connection URL: protocol://user[:password]@server/database")
          )
      }
  }

  object cache {
    val protocol = "([^:\\/]+:\\/\\/)"

    val userInfo = "(([^:]+):)?([^@]+)?@"

    val serverInstanceInfo = "([^\\/:]+):(\\d+)(\\/(.+))?"

    val CacheUrl = s"$protocol($userInfo)?($serverInstanceInfo)?".r

    case class CacheConnectionInfo(secret: Option[String], host: String, port: Int)

    def parseConnectionString(raw: String): ValidatedNel[ConfigError, CacheConnectionInfo] =
      raw match {
        case CacheUrl(_, _, _, _, s, _, h, p, _, _) ⇒

          val secret: ValidatedNel[ConfigError, Option[String]] = Validated.valid(Option(s))
          val host = Option(h).toValidNel[ConfigError](MissingConfigValue("Cache host missing"))
          val port: ValidatedNel[ConfigError, Int] =
            Option(p)
              .toValidNel[ConfigError](MissingConfigValue("Cache port missing"))
              .andThen(validatedString2Int)

          (secret |@| host |@| port) map { (secretValue, hostValue, portValue) ⇒
            CacheConnectionInfo(
              secret = secretValue,
              host   = hostValue,
              port   = portValue
            )

          }

        case _ ⇒
          Validated.invalidNel(
            UnexpectedConnectionURL("Unexpected cache connection URL: protocol://[[user:]secret@]server:port[/database]")
          )
      }
  }

}
