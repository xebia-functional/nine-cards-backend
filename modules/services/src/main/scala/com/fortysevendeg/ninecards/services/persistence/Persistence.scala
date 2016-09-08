package com.fortysevendeg.ninecards.services.persistence

import doobie.imports.{ Composite, ConnectionIO, Query, Query0, Update, Update0 }
import shapeless.HNil
import scalaz.Foldable

class Persistence[K: Composite](val supportsSelectForUpdate: Boolean = true) {

  def generateQuery(sql: String): Query0[K] =
    Query[HNil, K](sql).toQuery0(HNil)

  class GenerateQuery[L] {
    def apply[A: Composite](sql: String, values: A)(implicit L: Composite[L]): Query0[L] =
      Query[A, L](sql).toQuery0(values)
  }

  def generateQuery = new GenerateQuery[K]

  def generateQueryFor[L] = new GenerateQuery[L]

  def generateUpdateWithGeneratedKeys[A: Composite](sql: String, values: A): Update0 =
    Update[A](sql).toUpdate0(values)

  def generateUpdate(sql: String): Update0 = Update0(sql, None)

  def fetchList(sql: String): ConnectionIO[List[K]] =
    Query[HNil, K](sql).toQuery0(HNil).to[List]

  class FetchList[L] {
    def apply[A: Composite](sql: String, values: A)(implicit K: Composite[L]): ConnectionIO[List[L]] =
      Query[A, L](sql).to[List](values)
  }

  def fetchList = new FetchList[K]

  def fetchListAs[L] = new FetchList[L]

  def fetchOption[A: Composite](sql: String, values: A): ConnectionIO[Option[K]] =
    Query[A, K](sql).option(values)

  def fetchUnique[A: Composite](sql: String, values: A): ConnectionIO[K] =
    Query[A, K](sql).unique(values)

  def update(sql: String): ConnectionIO[Int] = Update[HNil](sql).run(HNil)

  def update[A](sql: String, values: A)(implicit A: Composite[A]): ConnectionIO[Int] =
    Update[A](sql).run(values)

  class UpdateWithGeneratedKeys[L] {

    def apply[A: Composite](sql: String, fields: List[String], values: A)(implicit K: Composite[L]): ConnectionIO[L] = {
      val prefix = if (supportsSelectForUpdate) fields else List("id")
      Update[A](sql).withUniqueGeneratedKeys[L](prefix: _*)(values)
    }
  }

  def updateWithGeneratedKeys[L] = new UpdateWithGeneratedKeys[L]

  def updateMany[F[_]: Foldable, A: Composite](sql: String, values: F[A]): ConnectionIO[Int] =
    Update[A](sql).updateMany(values)

}

object Persistence {

  implicit def persistence[K: Composite]: Persistence[K] = new Persistence
}
