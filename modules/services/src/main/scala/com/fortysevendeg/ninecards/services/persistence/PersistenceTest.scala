package com.fortysevendeg.ninecards.services.persistence

import doobie.imports._

object PersistenceTest {

  case class PersistenceItem(id: Long, name: String)

  class PersistenceServices(implicit persistence: PersistenceImpl) {

    val addItemSql = "insert into persistence (name) values (?)"
    val getItemSql = "select id, name from persistence where id = ?"
    val fields = List("id")

    def addItem(name: String): ConnectionIO[Long] = persistence.updateWithGeneratedKeys[String, Long](
      sql = addItemSql,
      fields = fields,
      values = name)

    def getItem(itemId: Long): ConnectionIO[Option[PersistenceItem]] =
      persistence.fetchOption[Long, PersistenceItem](
        sql = getItemSql,
        values = itemId)

    def createTable: ConnectionIO[Int] =
      sql"""
            CREATE TABLE persistence (
            id   BIGINT AUTO_INCREMENT,
            name VARCHAR NOT NULL UNIQUE)
        """.update.run

    def dropTable: ConnectionIO[Int] =
      sql"""
            DROP TABLE IF EXISTS persistence
        """.update.run
  }

  object PersistenceServices {

    implicit def persistenceServices(implicit persistence: PersistenceImpl): PersistenceServices = new PersistenceServices

  }

}
