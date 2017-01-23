package cards.nine.commons

import cats.data.{ NonEmptyList, Validated, ValidatedNel }

package object config {

  sealed abstract class ConfigError extends Serializable with Product

  final case class UnexpectedConnectionURL(message: String) extends ConfigError

  final case class MissingConfigValue(message: String) extends ConfigError

  final case class WrongFormat(message: String) extends ConfigError

  val validatedString2Int: String ⇒ ValidatedNel[ConfigError, Int] = { value: String ⇒
    Validated
      .catchNonFatal(value.toInt)
      .leftMap(_ ⇒ NonEmptyList.of(WrongFormat("The config value is not a number")))
  }
}
