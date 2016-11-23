package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cats.free.:<:

object GooglePlay {

  sealed trait Ops[A]

  case class Resolve(packageName: Package, auth: MarketCredentials)
    extends Ops[Result[FullCard]]

  case class ResolveManyBasic(packageNames: List[Package], auth: MarketCredentials)
    extends Ops[Result[CardList[BasicCard]]]

  case class ResolveManyDetailed(packageNames: List[Package], auth: MarketCredentials)
    extends Ops[Result[CardList[FullCard]]]

  case class RecommendationsByCategory(
    category: String,
    priceFilter: PriceFilter,
    excludesPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Result[CardList[FullCard]]]

  case class RecommendationsForApps(
    packagesName: List[Package],
    excludesPackages: List[Package],
    limitPerApp: Int,
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Result[CardList[FullCard]]]

  case class SearchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Result[CardList[BasicCard]]]

  case class ResolvePendingApps(numPackages: Int) extends Ops[Result[ResolvePendingStats]]

  case class StoreCard(card: FullCard) extends Ops[Result[Unit]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def resolve(
      packageName: Package,
      auth: MarketCredentials
    ): NineCardsService[F, FullCard] =
      NineCardsService(Resolve(packageName, auth))

    def resolveManyBasic(
      packageNames: List[Package],
      auth: MarketCredentials
    ): NineCardsService[F, CardList[BasicCard]] =
      NineCardsService(ResolveManyBasic(packageNames, auth))

    def resolveManyDetailed(
      packageNames: List[Package],
      auth: MarketCredentials
    ): NineCardsService[F, CardList[FullCard]] =
      NineCardsService(ResolveManyDetailed(packageNames, auth))

    def recommendByCategory(
      category: String,
      priceFilter: PriceFilter,
      excludesPackages: List[Package],
      limit: Int,
      auth: MarketCredentials
    ): NineCardsService[F, CardList[FullCard]] =
      NineCardsService(RecommendationsByCategory(category, priceFilter, excludesPackages, limit, auth))

    def recommendationsForApps(
      packagesName: List[Package],
      excludesPackages: List[Package],
      limitPerApp: Int,
      limit: Int,
      auth: MarketCredentials
    ): NineCardsService[F, CardList[FullCard]] =
      NineCardsService(RecommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth))

    def searchApps(
      query: String,
      excludesPackages: List[Package],
      limit: Int,
      auth: MarketCredentials
    ): NineCardsService[F, CardList[BasicCard]] =
      NineCardsService(SearchApps(query, excludesPackages, limit, auth))

    def resolvePendingApps(numPackages: Int): NineCardsService[F, ResolvePendingStats] =
      NineCardsService(ResolvePendingApps(numPackages))

    def storeCard(card: FullCard): NineCardsService[F, Unit] =
      NineCardsService(StoreCard(card))

  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] =
      new Services

  }
}

