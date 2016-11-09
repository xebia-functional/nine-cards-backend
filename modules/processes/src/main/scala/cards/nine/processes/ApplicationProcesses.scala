package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.NineCardsService
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package, ResolvePendingStats }
import cards.nine.domain.market.MarketCredentials
import cards.nine.processes.converters.Converters._
import cards.nine.services.free.algebra.GooglePlay

class ApplicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getAppsInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[FullCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil))
    else
      services.resolveManyDetailed(
        packageNames = packagesName,
        auth         = marketAuth
      ) map filterCategorized

  def getAppsBasicInfo(
    packagesName: List[Package],
    marketAuth: MarketCredentials
  ): NineCardsService[F, CardList[BasicCard]] =
    if (packagesName.isEmpty)
      NineCardsService.right(CardList(Nil, Nil))
    else
      services.resolveManyBasic(packagesName, marketAuth)

  def resolvePendingApps(numPackages: Int): NineCardsService[F, ResolvePendingStats] =
    services.resolvePendingApps(numPackages)

}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new ApplicationProcesses

}
