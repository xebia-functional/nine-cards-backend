package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}
import com.fortysevendeg.ninecards.services.free.domain.CategorizeResponse

import scala.language.higherKinds

object AppPersistence {

  sealed trait AppPersistenceOps[A]

  case class GetCategories(packageNames: Seq[String]) extends AppPersistenceOps[CategorizeResponse]

  case class SaveCategories(packageNames: Seq[String]) extends AppPersistenceOps[Seq[String]]

  class AppPersistenceServices[F[_]](implicit I: Inject[AppPersistenceOps, F]) {

    def getCategories(
      packageNames: Seq[String]): Free[F, CategorizeResponse] =
      Free.inject[AppPersistenceOps, F](GetCategories(packageNames))

    def saveCategories(
      packageNames: Seq[String]): Free[F, Seq[String]] =
      Free.inject[AppPersistenceOps, F](SaveCategories(packageNames))
  }

  object AppPersistenceServices {

    implicit def appPersistence[F[_]](
      implicit I: Inject[AppPersistenceOps, F]): AppPersistenceServices[F] =
      new AppPersistenceServices[F]

  }

}
