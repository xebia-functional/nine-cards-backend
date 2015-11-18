package com.fortysevendeg.ninecards.free.algebra

import scalaz.Free._
import scalaz.{Free, Inject}

object appsPersistence {

  def lift[F[_], G[_], A](fa: F[A])(implicit I: Inject[F, G]): FreeC[G, A] = Free.liftFC(I.inj(fa))

  sealed trait AppPersistenceOps[A]

  case class GetCategories(packageNames: Seq[String]) extends AppPersistenceOps[Seq[String]]

  case class SaveCategories(packageNames: Seq[String]) extends AppPersistenceOps[Seq[String]]

  class AppPersistenceServices[F[_]](implicit I: Inject[AppPersistenceOps, F]) {

    def getCategories(packageNames: Seq[String]): Free.FreeC[F, Seq[String]] = lift[AppPersistenceOps, F, Seq[String]](GetCategories(packageNames))

    def saveCategories(packageNames: Seq[String]): Free.FreeC[F, Seq[String]] = lift[AppPersistenceOps, F, Seq[String]](SaveCategories(packageNames))
  }

  object AppPersistenceServices {

    implicit def appPersistence[F[_]](implicit I: Inject[AppPersistenceOps, F]): AppPersistenceServices[F] = new AppPersistenceServices[F]

  }
}
