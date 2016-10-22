package cards.nine.commons

object NineCardsErrors {

  sealed abstract class NineCardsError extends Serializable with Product

  final case class CountryNotFound(message: String) extends NineCardsError

  final case class GoogleAnalyticsServerError(message: String) extends NineCardsError

  final case class HttpBadRequest(message: String) extends NineCardsError

  final case class HttpNotFound(message: String) extends NineCardsError

  final case class HttpUnauthorized(message: String) extends NineCardsError

  final case class ReportNotFound(message: String) extends NineCardsError
}
