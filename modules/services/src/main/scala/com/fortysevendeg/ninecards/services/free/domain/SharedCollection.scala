package com.fortysevendeg.ninecards.services.free.domain

import java.sql.Timestamp

case class SharedCollection(
  id: Long,
  publicIdentifier: String,
  userId: Option[Long],
  publishedOn: Timestamp,
  author: String,
  name: String,
  installations: Int,
  views: Int,
  category: String,
  icon: String,
  community: Boolean
)

case class SharedCollectionPackage(
  id: Long,
  sharedCollectionId: Long,
  packageName: String
)

case class SharedCollectionSubscription(
  id: Long,
  sharedCollectionId: Long,
  userId: Long
)

object SharedCollection {
  val allFields = List("id", "publicidentifier", "userid", "publishedon", "author", "name",
    "installations", "views", "category", "icon", "community")

  object Queries {
    val getById = "select * from sharedcollections where id=?"
    val getByPublicIdentifier = "select * from sharedcollections where publicidentifier=?"
    val getByUser = "select * from sharedcollections where userId=?"
    val getLatestByCategory = "select * from sharedcollections where category=? order by publishedon desc limit ? offset ?"
    val getTopByCategory = "select * from sharedcollections where category=? order by installations desc limit ? offset ?"
    val insert = "insert into sharedcollections(publicidentifier,userid,publishedon,author,name,installations,views,category,icon,community) values(?,?,?,?,?,?,?,?,?,?)"
    val update = "update sharedcollections set name=? where id=?"
  }
}

object SharedCollectionPackage {
  val allFields = List("id", "sharedcollectionid", "packagename")

  object Queries {
    val delete = "delete from sharedcollectionpackages where sharedcollectionid=? and packagename=?"
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