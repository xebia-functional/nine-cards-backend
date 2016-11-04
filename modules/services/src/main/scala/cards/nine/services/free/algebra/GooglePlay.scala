package cards.nine.services.free.algebra

import cards.nine.domain.application._
import cards.nine.domain.market.MarketCredentials
import cats.data.Xor
import cats.free.{ Free, Inject }

object GooglePlay {

  sealed trait Ops[A]

  case class Resolve(packageName: Package, auth: MarketCredentials)
    extends Ops[String Xor FullCard]

  case class ResolveManyBasic[A](packageNames: List[Package], auth: MarketCredentials)
    extends Ops[CardList[BasicCard]]

  case class ResolveManyDetailed[A](packageNames: List[Package], auth: MarketCredentials)
    extends Ops[CardList[FullCard]]

  case class RecommendationsByCategory(
    category: String,
    priceFilter: PriceFilter,
    excludesPackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[FullCardList]

  case class RecommendationsForApps(
    packagesName: List[Package],
    excludesPackages: List[Package],
    limitPerApp: Int,
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[FullCardList]

  case class SearchApps(
    query: String,
    excludePackages: List[Package],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[BasicCardList]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(
      packageName: Package,
      auth: MarketCredentials
    ): Free[F, String Xor FullCard] =
      Free.inject[Ops, F](Resolve(packageName, auth))

    def resolveManyBasic(
      packageNames: List[Package],
      auth: MarketCredentials
    ): Free[F, BasicCardList] =
      Free.inject[Ops, F](ResolveManyBasic(packageNames, auth))

    def resolveManyDetailed(
      packageNames: List[Package],
      auth: MarketCredentials
    ): Free[F, FullCardList] =
      Free.inject[Ops, F](ResolveManyDetailed(packageNames, auth))

    def recommendByCategory(
      category: String,
      priceFilter: PriceFilter,
      excludesPackages: List[Package],
      limit: Int,
      auth: MarketCredentials
    ): Free[F, FullCardList] =
      Free.inject[Ops, F](RecommendationsByCategory(category, priceFilter, excludesPackages, limit, auth))

    def recommendationsForApps(
      packagesName: List[Package],
      excludesPackages: List[Package],
      limitPerApp: Int,
      limit: Int,
      auth: MarketCredentials
    ): Free[F, FullCardList] =
      Free.inject[Ops, F](RecommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth))

    def searchApps(
      query: String,
      excludesPackages: List[Package],
      limit: Int,
      auth: MarketCredentials
    ): Free[F, BasicCardList] =
      Free.inject[Ops, F](SearchApps(query, excludesPackages, limit, auth))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }
}

