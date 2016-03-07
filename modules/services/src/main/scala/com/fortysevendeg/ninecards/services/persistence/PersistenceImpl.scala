package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._
import shapeless.HNil

import scalaz.Foldable

class PersistenceImpl[K](val supportsSelectForUpdate: Boolean = true) {

  def generateQuery(sql: String)(implicit K: Composite[K]): Query0[K] =
    Query[HNil, K](sql).toQuery0(HNil)

  def generateQuery[A](
    sql: String, values: A)(
    implicit A: Composite[A],
    K: Composite[K]): Query0[K] = Query[A, K](sql).toQuery0(values)

  def generateUpdateWithGeneratedKeys[A](
    sql: String, values: A)(
    implicit A: Composite[A]): Update0 = Update[A](sql).toUpdate0(values)

  def fetchList(
    sql: String)(
    implicit K: Composite[K]): ConnectionIO[List[K]] = Query[HNil, K](sql).toQuery0(HNil).to[List]

  def fetchList[A](
    sql: String, values: A)(
    implicit A: Composite[A],
    K: Composite[K]): ConnectionIO[List[K]] = Query[A, K](sql).to[List](values)

  def fetchOption[A](
    sql: String,
    values: A)(
    implicit A: Composite[A],
    K: Composite[K]): ConnectionIO[Option[K]] = Query[A, K](sql).option(values)

  def fetchUnique[A](
    sql: String, values: A)(
    implicit A: Composite[A],
    K: Composite[K]): ConnectionIO[K] = Query[A, K](sql).unique(values)

  def update(sql: String): ConnectionIO[Int] = Update[HNil](sql).run(HNil)

  def update[A](
    sql: String,
    values: A)(
    implicit A: Composite[A]): ConnectionIO[Int] = Update[A](sql).run(values)

  def updateWithGeneratedKeys[L] = new {
    def apply[A](sql: String, fields: List[String], values: A)(
      implicit A: Composite[A], K: Composite[L]): ConnectionIO[L] = {
      if (supportsSelectForUpdate)
        Update[A](sql).withUniqueGeneratedKeys[L](fields: _*)(values)
      else
        Update[A](sql).withUniqueGeneratedKeys[L](List("id"): _*)(values)
    }
  }

  def updateMany[F[_], A](
    sql: String,
    values: F[A])(
    implicit A: Composite[A],
    F: Foldable[F]): ConnectionIO[Int] = Update[A](sql).updateMany(values)

}

object PersistenceImpl {

  implicit def persistenceImpl[K]: PersistenceImpl[K] = new PersistenceImpl
}
