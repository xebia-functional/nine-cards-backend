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

  def getUser(user: User) = user

  def getUserByEmail(email: String) =
    Option(
      User(
        id = 32132165,
        sessionToken = "asjdfoaijera",
        email = "ana@47deg.com",
        banned = false
      )
    )

  def getDeviceByAndroidId(androidId: String) = {
    Option(
      Device(
        id = 51654,
        userId = 46546456,
        androidId = androidId)
    )
  }

  def createDevice(userId: Long, androidId: String) =
    Device(
      userId = userId,
      id = 98798,
      androidId = androidId
    )

  def getDevice(device: Device) = device


  //  def createInstallation(installation: Installation) =
  //    Installation(
  //      userId = installation.userId,
  //      id = Option("340520945234109234527345")
  //    )
  //
  //  def updateInstallation(installation: Installation, installationId: String) =
  //    Installation(
  //      id = Option(installationId),
  //      userId = installation.userId,
  //      deviceToken = installation.deviceToken
  //    )

}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
