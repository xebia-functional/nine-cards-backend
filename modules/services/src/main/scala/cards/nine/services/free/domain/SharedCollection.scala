package cards.nine.services.free.domain

import java.sql.Timestamp

sealed abstract class BaseSharedCollection {
  def sharedCollectionId: Long
}

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
) extends BaseSharedCollection {
  override def sharedCollectionId: Long = id
}

case class SharedCollectionWithAggregatedInfo(
  sharedCollectionData: SharedCollection,
  subscriptionsCount: Long
) extends BaseSharedCollection {
  override def sharedCollectionId: Long = sharedCollectionData.id
}

case class SharedCollectionPackage(
  id: Long,
  sharedCollectionId: Long,
  packageName: String
)

case class SharedCollectionSubscription(
  sharedCollectionId: Long,
  userId: Long,
  sharedCollectionPublicId: String
)

object SharedCollection {
  val fields = List("publicidentifier", "userid", "publishedon", "author", "name",
    "installations", "views", "category", "icon", "community")
  val allFields = "id" +: fields

  val insertFields = fields.mkString(",")
  val insertWildCards = fields.map(_ ⇒ "?").mkString(",")

  object Queries {
    val getById = "select * from sharedcollections where id=?"
    val getByPublicIdentifier = "select * from sharedcollections where publicidentifier=?"
    val getByUser =
      s"""
        |select C.*, count(S.*) as subscriptionCount
        |from sharedcollections as C
        |left join sharedcollectionsubscriptions as S on C.id=S.sharedcollectionid
        |where C.userid=?
        |group by C.id""".stripMargin
    val getLatestByCategory = "select * from sharedcollections where category=? order by publishedon desc limit ? offset ?"
    val getTopByCategory = "select * from sharedcollections where category=? order by installations desc limit ? offset ?"
    val insert = s"insert into sharedcollections($insertFields) values($insertWildCards)"
    val update = "update sharedcollections set name=? where id=?"
  }
}

object SharedCollectionPackage {
  val fields = List("sharedcollectionid", "packagename")
  val allFields = "id" +: fields

  val insertFields = fields.mkString(",")
  val insertWildCards = fields.map(_ ⇒ "?").mkString(",")

  object Queries {
    val delete = "delete from sharedcollectionpackages where sharedcollectionid=? and packagename=?"
    val getById = "select * from sharedcollectionpackages where id=?"
    val getBySharedCollection = "select * from sharedcollectionpackages where sharedcollectionid=?"
    val insert = s"insert into sharedcollectionpackages($insertFields) values($insertWildCards)"
  }
}

object SharedCollectionSubscription {
  val fields = List("sharedcollectionid", "userid", "sharedcollectionpublicid")

  val insertFields = fields.mkString(",")
  val insertWildCards = fields.map(_ ⇒ "?").mkString(",")

  object Queries {
    val getByCollection = "select * from sharedcollectionsubscriptions where sharedcollectionid=?"
    val getByCollectionAndUser = "select * from sharedcollectionsubscriptions where sharedcollectionid=? and userid=?"
    val getByUser = "select * from sharedcollectionsubscriptions where userid=?"
    val insert = s"insert into sharedcollectionsubscriptions($insertFields) values($insertWildCards)"
    val deleteByCollectionAndUser = "delete from sharedcollectionsubscriptions where sharedcollectionid=? and userid=?"
  }
}