package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain._

class UserPersistenceImpl {

  def addUser(user: User) = user

  def getUserByEmail(email: String) =
    Option(
      User(
        id = Option("32132165"),
        sessionToken = "asjdfoaijera"
      )
    )

  def createInstallation(installation: Installation) =
    Installation(
      userId = installation.userId,
      id = Option("340520945234109234527345")
    )

  def updateInstallation(installation: Installation, installationId: String) =
    Installation(
      id = Option(installationId),
      userId = installation.userId,
      deviceToken = installation.deviceToken
    )

}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
