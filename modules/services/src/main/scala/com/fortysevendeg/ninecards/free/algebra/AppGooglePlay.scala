package com.fortysevendeg.ninecards.free.algebra

import scalaz.Free._
import scalaz.{Monad, Free, Inject}

object appsGooglePlay {

  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] = Free.liftFC(I.inj(fa))

  sealed trait AppGooglePlayOps[A]

  case class GetCategoriesFromGooglePlay(packageNames: Seq[String]) extends AppGooglePlayOps[Seq[String]]

  class AppGooglePlayServices[F[_]](implicit I: Inject[AppGooglePlayOps, F]) {

    def getCategoriesFromGooglePlay(packageNames: Seq[String]): Free.FreeC[F, Seq[String]] = lift[AppGooglePlayOps, F, Seq[String]](GetCategoriesFromGooglePlay(packageNames))

  }

  object AppGooglePlayServices {

    implicit def appGooglePlay[F[_]](implicit I: Inject[AppGooglePlayOps, F]): AppGooglePlayServices[F] = new AppGooglePlayServices[F]

  }
}
