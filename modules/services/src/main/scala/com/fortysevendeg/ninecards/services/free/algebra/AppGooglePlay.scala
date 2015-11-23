package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.algebra.Utils._
import com.fortysevendeg.ninecards.services.free.domain.CategorizeResponse

import scala.language.higherKinds

object AppGooglePlay {

  sealed trait AppGooglePlayOps[A]

  case class GetCategoriesFromGooglePlay(packageNames: Seq[String]) extends AppGooglePlayOps[CategorizeResponse]

  class AppGooglePlayServices[F[_]](implicit I: Inject[AppGooglePlayOps, F]) {

    def getCategoriesFromGooglePlay(
      packageNames: Seq[String]): Free[F, CategorizeResponse] =
      lift[AppGooglePlayOps, F, CategorizeResponse](GetCategoriesFromGooglePlay(packageNames))

  }

  object AppGooglePlayServices {

    implicit def appGooglePlay[F[_]](implicit I: Inject[AppGooglePlayOps, F]): AppGooglePlayServices[F] = new AppGooglePlayServices[F]

  }

}
