package cards.nine.services.free.algebra

import cards.nine.domain.market.MarketCredentials
import cards.nine.services.free.domain.GooglePlay._
import cats.data.Xor
import cats.free.{ Free, Inject }

object GooglePlay {

  sealed trait Ops[A]

  case class Resolve(packageName: String, auth: MarketCredentials)
    extends Ops[String Xor AppInfo]

  case class ResolveMany(packageNames: List[String], auth: MarketCredentials, extendedInfo: Boolean)
    extends Ops[AppsInfo]

  case class RecommendationsByCategory(
    category: String,
    priceFilter: String,
    excludesPackages: List[String],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Recommendations]

  case class RecommendationsForApps(
    packagesName: List[String],
    excludesPackages: List[String],
    limitPerApp: Int,
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Recommendations]

  case class SearchApps(
    query: String,
    excludePackages: List[String],
    limit: Int,
    auth: MarketCredentials
  ) extends Ops[Recommendations]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(packageName: String, auth: MarketCredentials): Free[F, String Xor AppInfo] =
      Free.inject[Ops, F](Resolve(packageName, auth))

    def resolveMany(
      packageNames: List[String],
      auth: MarketCredentials,
      extendedInfo: Boolean
    ): Free[F, AppsInfo] =
      Free.inject[Ops, F](ResolveMany(packageNames, auth, extendedInfo))

    def recommendByCategory(
      category: String,
      priceFilter: String,
      excludesPackages: List[String],
      limit: Int,
      auth: MarketCredentials
    ): Free[F, Recommendations] =
      Free.inject[Ops, F](RecommendationsByCategory(category, priceFilter, excludesPackages, limit, auth))

    def recommendationsForApps(
      packagesName: List[String],
      excludesPackages: List[String],
      limitPerApp: Int,
      limit: Int,
      auth: MarketCredentials
    ): Free[F, Recommendations] =
      Free.inject[Ops, F](RecommendationsForApps(packagesName, excludesPackages, limitPerApp, limit, auth))

    def searchApps(
      query: String,
      excludesPackages: List[String],
      limit: Int,
      auth: MarketCredentials
    ): Free[F, Recommendations] =
      Free.inject[Ops, F](SearchApps(query, excludesPackages, limit, auth))

  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }
}

