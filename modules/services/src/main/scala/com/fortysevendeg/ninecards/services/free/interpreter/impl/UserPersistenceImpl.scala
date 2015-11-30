package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.User

class UserPersistenceImpl {

  def addUser(user: User) = user

  def getUserByUserName(username: String) = Option(User())

  def checkPassword(pass: String) = true

}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
