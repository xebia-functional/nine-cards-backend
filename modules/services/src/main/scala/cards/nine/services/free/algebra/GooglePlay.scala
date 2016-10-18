package cards.nine.services.free.algebra

import cards.nine.domain.application.{ FullCard, FullCardList, Package }
import cards.nine.domain.market.MarketCredentials
import cats.data.Xor
import cats.free.{ Free, Inject }

object GooglePlay {

  sealed trait Ops[A]

  case class Resolve(packageName: Package, auth: MarketCredentials)
    extends Ops[String Xor FullCard]

  case class ResolveMany(packageNames: List[Package], auth: MarketCredentials, extendedInfo: Boolean)
    extends Ops[FullCardList]

  case class RecommendationsByCategory(
    category: String,
    priceFilter: String,
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
  ) extends Ops[FullCardList]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(packageName: Package, auth: MarketCredentials): Free[F, String Xor FullCard] =
      Free.inject[Ops, F](Resolve(packageName, auth))

    def resolveMany(
      packageNames: List[Package],
      auth: MarketCredentials,
      extendedInfo: Boolean
    ): Free[F, FullCardList] =
      Free.inject[Ops, F](ResolveMany(packageNames, auth, extendedInfo))

    def recommendByCategory(
      category: String,
      priceFilter: String,
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
    ): Free[F, FullCardList] =
      Free.inject[Ops, F](SearchApps(query, excludesPackages, limit, auth))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }
}

