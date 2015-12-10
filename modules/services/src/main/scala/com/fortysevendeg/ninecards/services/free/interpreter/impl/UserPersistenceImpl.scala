package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.{GoogleAuthDataDeviceInfo, GoogleAuthData, AuthData, User}

class UserPersistenceImpl {

  def addUser(user: User) = user

  def getUserByUserName(username: String) = Option(User())

  def checkPassword(pass: String) = true

  def getUserByUserId(userId: String) =
    Option(
      User(
        _id = Option("123"),
        username = Option("Ana"),
        email = Option("ana@47deg.com"),
        sessionToken = Option("asjdfoaijerña"),
        authData = Option(AuthData(
          google = Option(GoogleAuthData(
            email = "ana@47deg.com",
            devices = List(
              GoogleAuthDataDeviceInfo(
                name = "aldfa",
                deviceId = "ladf",
                secretToken = "lakjdsflkadf",
                permissions = Nil
              )
            ))
          ))
        )
      )
    )
}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
