package com.fortysevendeg.ninecards.services.free.algebra

import com.fortysevendeg.ninecards.services.free.algebra.Utils._
import com.fortysevendeg.ninecards.services.free.domain.CategorizeResponse

import scalaz.{Free, Inject}

object AppGooglePlay {

  sealed trait AppGooglePlayOps[A]

  case class GetCategoriesFromGooglePlay(packageNames: Seq[String]) extends AppGooglePlayOps[CategorizeResponse]

  class AppGooglePlayServices[F[_]](implicit I: Inject[AppGooglePlayOps, F]) {

    def getCategoriesFromGooglePlay(
      packageNames: Seq[String]): Free.FreeC[F, CategorizeResponse] =
      lift[AppGooglePlayOps, F, CategorizeResponse](GetCategoriesFromGooglePlay(packageNames))

  }

  object AppGooglePlayServices {

    implicit def appGooglePlay[F[_]](implicit I: Inject[AppGooglePlayOps, F]): AppGooglePlayServices[F] = new AppGooglePlayServices[F]

  }

}
