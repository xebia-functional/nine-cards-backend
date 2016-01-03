package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._

class PersistenceImpl {

  def fetchList[K](
    sql: String)(implicit ev: Composite[K]): ConnectionIO[List[K]] = sql"$sql".query[K].list

  def fetchList[A, K](
    sql: String,
    values: A)(implicit ev: Composite[A], ev2: Composite[K]): ConnectionIO[List[K]] =
    Query[A, K](sql).to[List](values)

  def fetchOption[A, K](
    sql: String,
    values: A)(implicit ev: Composite[A], ev2: Composite[K]): ConnectionIO[Option[K]] =
    Query[A, K](sql).option(values)

  def fetchUnique[A, K](
    sql: String,
    values: A)(implicit ev: Composite[A], ev2: Composite[K]): ConnectionIO[K] =
    Query[A, K](sql).unique(values)

  def update[A, K](
    sql: String,
    fields: Seq[String],
    values: A)(implicit ev: Composite[A], ev2: Composite[K]): ConnectionIO[K] =
    Update[A](sql).withUniqueGeneratedKeys[K](fields: _*)(values)

  def updateMany[A, K](
    sql: String,
    fields: Seq[String],
    values: A)(implicit ev: Composite[A], ev2: Composite[K]): ConnectionIO[List[K]] =
    Update[A](sql).withGeneratedKeys[K](fields: _*)(values).list

}

object PersistenceImpl {

  implicit def persistenceImpl: PersistenceImpl = new PersistenceImpl
}
