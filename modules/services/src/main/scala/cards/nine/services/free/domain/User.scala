package cards.nine.services.free.domain

case class User(
  id: Long,
  email: String,
  sessionToken: String,
  apiKey: String,
  banned: Boolean
)

case class Installation(
  id: Long,
  userId: Long,
  deviceToken: Option[String] = None,
  androidId: String
)

object User {
  val allFields = List("id", "email", "sessiontoken", "apikey", "banned")

  object Queries {
    val getByEmail = "select * from users where email=?"
    val getBySessionToken = "select * from users where sessiontoken=?"
    val insert = "insert into users(email,sessiontoken,apikey) values(?,?,?)"
  }

}

object Installation {
  val fields = List("userid", "devicetoken", "androidid")
  val allFields = "id" +: fields

  val insertFields = fields.mkString(",")
  val insertWildCards = fields.map(_ ⇒ "?").mkString(",")

  object Queries {
    val getByUserAndAndroidId = "select * from installations where userid=? and androidid=?"
    val getById = "select * from installations where id=?"
    val getSubscribedByCollection =
      """
        |select I.*
        |from installations as I
        |inner join users as U on U.id=I.userid
        |inner join sharedcollectionsubscriptions as S on U.id=S.userid
        |where S.sharedCollectionPublicId=?
        |""".stripMargin
    val insert = s"insert into installations($insertFields) values($insertWildCards)"
    val updateDeviceToken = "update installations set devicetoken=? where userid=? and androidid=?"
  }

}