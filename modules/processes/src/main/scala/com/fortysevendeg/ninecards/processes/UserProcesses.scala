package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.domain.User
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollections.SharedCollectionServices
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscriptions.SharedCollectionSubscriptionServices
import com.fortysevendeg.ninecards.services.free.algebra.Users._

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userSevices: UserServices[F]) {

  import userSevices._

  def getUserById(userId: String): Free[F, User] = for {
    persistenceApps <- getUserByIdUser(userId)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit U: UserServices[F], SC: SharedCollectionServices[F], SCS: SharedCollectionSubscriptionServices[F]) = new UserProcesses()

}
