package com.fortysevendeg.ninecards.services.free.algebra

import cats.data.Xor
import cats.free.{ Free, Inject }
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay._

object GooglePlay {

  sealed trait Ops[A]

  case class Resolve(packageName: String, auth: AuthParams)
    extends Ops[String Xor AppInfo]

  case class ResolveMany(packageNames: Seq[String], auth: AuthParams)
    extends Ops[AppsInfo]

  case class RecommendationsByCategory(category: String, priceFilter: String, auth: AuthParams)
    extends Ops[Recommendations]

  case class RecommendationsForApps(packagesName: List[String], auth: AuthParams)
    extends Ops[Recommendations]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def resolve(packageName: String, auth: AuthParams): Free[F, String Xor AppInfo] =
      Free.inject[Ops, F](Resolve(packageName, auth))

    def resolveMany(packageNames: Seq[String], auth: AuthParams): Free[F, AppsInfo] =
      Free.inject[Ops, F](ResolveMany(packageNames, auth))

    def recommendByCategory(category: String, priceFilter: String, auth: AuthParams): Free[F, Recommendations] =
      Free.inject[Ops, F](RecommendationsByCategory(category, priceFilter, auth))

    def recommendationsForApps(packagesName: List[String], auth: AuthParams): Free[F, Recommendations] =
      Free.inject[Ops, F](RecommendationsForApps(packagesName, auth))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] =
      new Services

  }
}

