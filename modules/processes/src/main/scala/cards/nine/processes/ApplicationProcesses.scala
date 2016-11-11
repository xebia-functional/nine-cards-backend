package cards.nine.processes

import cards.nine.commons.FreeUtils._
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package, ResolvePendingStats }
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.converters.Converters._
import cards.nine.services.free.algebra.GooglePlay
import cats.free.Free

class ApplicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getAppsInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): Free[F, CardList[FullCard]] =
    if (packagesName.isEmpty)
      CardList(Nil, Nil).toFree
    else
      services.resolveManyDetailed(
        packageNames = packagesName,
        auth         = marketAuth
      ) map filterCategorized

  def getAppsBasicInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): Free[F, CardList[BasicCard]] =
    if (packagesName.isEmpty)
      CardList(Nil, Nil).toFree
    else
      services.resolveManyBasic(packagesName, marketAuth) // WE CANNOT FILTER CATEGORIZED: NO CATEGORIES

  def resolvePendingApps(numPackages: Int): Free[F, ResolvePendingStats] =
    services.resolvePendingApps(numPackages)

}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new ApplicationProcesses

}
