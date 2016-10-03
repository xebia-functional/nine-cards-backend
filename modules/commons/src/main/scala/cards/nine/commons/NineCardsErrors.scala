package cards.nine.commons

object NineCardsErrors {

  sealed abstract class NineCardsError extends Serializable with Product

  final case class CountryNotFound(message: String) extends NineCardsError
}
