package cards.nine.googleplay.domain

package webscrapper {

  sealed trait Failure
  case class PackageNotFound(pack: Package) extends Failure
  case class PageParseFailed(pack:Package) extends Failure
  case object WebPageServerError extends Failure

}
