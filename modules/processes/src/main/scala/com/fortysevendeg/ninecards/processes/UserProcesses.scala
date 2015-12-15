package com.fortysevendeg.ninecards.processes

import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionServices
import com.fortysevendeg.ninecards.services.free.algebra.Users.UserServices

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userSevices: UserServices[F]) {

  def getUserById(userId: String): Free[F, User] = for {
    persistenceApps <- userSevices.getUserById(userId)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

  def addUser(user: AddUserRequest): Free[F, User] = for {
    persistenceApps <- userServices.addUser(user)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userSevices: UserServices[F]) = new UserProcesses()
  
}
