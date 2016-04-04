package com.fortysevendeg.ninecards.services.free.domain

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
  val allFields = List("id", "userid", "devicetoken", "androidid")

  object Queries {
    val getByUserAndAndroidId = "select * from installations where userid=? and androidid=?"
    val getById = "select * from installations where id=?"
    val insert = "insert into installations(userid,devicetoken,androidid) values(?,?,?)"
    val updateDeviceToken = "update installations set devicetoken=? where userid=? and androidid=?"
  }

}