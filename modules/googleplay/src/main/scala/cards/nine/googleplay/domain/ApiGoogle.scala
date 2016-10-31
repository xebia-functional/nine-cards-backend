package cards.nine.googleplay.domain

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.MarketCredentials

package apigoogle {

  sealed trait Failure

  case class PackageNotFound(pack: Package) extends Failure
  case class WrongAuthParams(auth: MarketCredentials) extends Failure
  case class QuotaExceeded(auth: MarketCredentials) extends Failure
  case object GoogleApiServerError extends Failure

  case class ResolvePackagesResult(
    cachedPackages: List[FullCard],
    resolvedPackages: List[FullCard],
    notFoundPackages: List[Package],
    pendingPackages: List[Package]
  )
}
