package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain._

class UserPersistenceImpl {

  def createUser(email: String, androidId: String, sessionToken: String) =
    User(
      id = 54654,
      sessionToken = sessionToken,
      email = email,
      banned = false
    )

  def getUserByEmail(email: String) =
    Option(
      User(
        id = 32132165,
        sessionToken = "asjdfoaijera",
        email = "ana@47deg.com",
        banned = false
      )
    )

  def getUser(user: User) = user

  def getInstallation(installation: Installation) = installation

  def getInstallationByAndroidId(androidId: String) = {
    Option(
      Installation(
        id = 51654,
        userId = 46546456,
        androidId = androidId)
    )
  }

  def createInstallation(userId: Long, androidId: String) =
    Installation(
      userId = userId,
      id = 98798,
      androidId = androidId
    )
}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
