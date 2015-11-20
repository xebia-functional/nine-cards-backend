package com.fortysevendeg.ninecards.services.free.algebra

import com.fortysevendeg.ninecards.services.free.algebra.Utils._
import com.fortysevendeg.ninecards.services.free.domain.CategorizeResponse

import scalaz.{Free, Inject}

object AppPersistence {

  sealed trait AppPersistenceOps[A]

  case class GetCategories(packageNames: Seq[String]) extends AppPersistenceOps[CategorizeResponse]

  case class SaveCategories(packageNames: Seq[String]) extends AppPersistenceOps[Seq[String]]

  class AppPersistenceServices[F[_]](implicit I: Inject[AppPersistenceOps, F]) {

    def getCategories(
      packageNames: Seq[String]): Free.FreeC[F, CategorizeResponse] =
      lift[AppPersistenceOps, F, CategorizeResponse](GetCategories(packageNames))

    def saveCategories(
      packageNames: Seq[String]): Free.FreeC[F, Seq[String]] =
      lift[AppPersistenceOps, F, Seq[String]](SaveCategories(packageNames))
  }

  object AppPersistenceServices {

    implicit def appPersistence[F[_]](implicit I: Inject[AppPersistenceOps, F]): AppPersistenceServices[F] = new AppPersistenceServices[F]

  }

}
