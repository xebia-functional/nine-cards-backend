package cards.nine.googleplay.domain

package apigoogle {

  sealed trait Failure

  case class PackageNotFound( pack: Package) extends Failure
  case class WrongAuthParams( auth: GoogleAuthParams) extends Failure
  case class QuotaExceeded(auth: GoogleAuthParams) extends Failure
  case object GoogleApiServerError extends Failure
}
