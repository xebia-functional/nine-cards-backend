package com.fortysevendeg.ninecards.services.free.domain

import java.sql.Timestamp

case class SharedCollection(
  id: Long,
  publicIdentifier: String,
  userId: Option[Long],
  publishedOn: Timestamp,
  description: Option[String],
  author: String,
  name: String,
  installations: Int,
  views: Int,
  category: String,
  icon: String,
  community: Boolean)

case class SharedCollectionPackage(
  id: Long,
  sharedCollectionId: Long,
  packageName: String)

case class SharedCollectionSubscription(
  id: Long,
  sharedCollectionId: Long,
  userId: Long)

object SharedCollection {
  val allFields = List("id", "publicidentifier", "userid", "publishedon", "description", "author",
    "name", "installations", "views", "category", "icon", "community")

  object Queries {
    val getById = "select * from sharedcollections where id=?"
    val getByPublicIdentifier = "select * from sharedcollections where publicidentifier=?"
    val insert = "insert into sharedcollections(publicidentifier,userid,publishedon,description,author,name,installations,views,category,icon,community) values(?,?,?,?,?,?,?,?,?,?,?)"
  }
}

object SharedCollectionPackage {
  val allFields = List("id", "sharedcollectionid", "packagename")

  object Queries {
    val getById = "select * from sharedcollectionpackages where id=?"
    val getBySharedCollection = "select * from sharedcollectionpackages where sharedcollectionid=?"
    val insert = "insert into sharedcollectionpackages(sharedcollectionid,packagename) values(?,?)"
  }
}

object SharedCollectionSubscription {
  val allFields = List("id", "sharedcollectionid", "userid")

  object Queries {
    val getById = "select * from sharedcollectionsubscriptions where id=?"
    val getByCollection = "select * from sharedcollectionsubscriptions where sharedcollectionid=?"
    val getByCollectionAndUser = "select * from sharedcollectionsubscriptions where sharedcollectionid=? and userid=?"
    val getByUser = "select * from sharedcollectionsubscriptions where userid=?"
    val insert = "insert into sharedcollectionsubscriptions(sharedcollectionid,userid) values(?,?)"
    val delete = "delete from sharedcollectionsubscriptions where id=?"
    val deleteByCollectionAndUser = "delete from sharedcollectionsubscriptions where sharedcollectionid=? and userid=?"
  }
}