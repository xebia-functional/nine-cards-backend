/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.free.domain

import cards.nine.domain.account._

case class User(
  id: Long,
  email: Email,
  sessionToken: SessionToken,
  apiKey: ApiKey,
  banned: Boolean
)

case class UserData(
  email: String,
  sessionToken: String,
  apiKey: String
)

case class Installation(
  id: Long,
  userId: Long,
  deviceToken: Option[DeviceToken] = None,
  androidId: AndroidId
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