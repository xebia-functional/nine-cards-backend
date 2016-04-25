package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import shapeless.HNil

import scalaz.Foldable

class Persistence[K](val supportsSelectForUpdate: Boolean = true) {

  def generateQuery(sql: String)(implicit K: Composite[K]): Query0[K] =
    Query[HNil, K](sql).toQuery0(HNil)

  def generateQuery[A: Composite](sql: String, values: A)(implicit K: Composite[K]): Query0[K] =
    Query[A, K](sql).toQuery0(values)

  def generateUpdateWithGeneratedKeys[A: Composite](sql: String, values: A): Update0 =
    Update[A](sql).toUpdate0(values)

  def fetchList(sql: String)(implicit K: Composite[K]): ConnectionIO[List[K]] =
    Query[HNil, K](sql).toQuery0(HNil).to[List]

  def fetchList[A: Composite](sql: String, values: A)(implicit K: Composite[K]): ConnectionIO[List[K]] =
    Query[A, K](sql).to[List](values)

  def fetchOption[A: Composite](sql: String, values: A)(implicit K: Composite[K]): ConnectionIO[Option[K]] =
    Query[A, K](sql).option(values)

  def fetchUnique[A: Composite](sql: String, values: A)(implicit K: Composite[K]): ConnectionIO[K] =
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

  implicit def persistence[K]: Persistence[K] = new Persistence
}
