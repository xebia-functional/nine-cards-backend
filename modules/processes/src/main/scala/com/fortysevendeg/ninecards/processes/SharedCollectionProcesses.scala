package com.fortysevendeg.ninecards.processes

import cats.data.Xor
import cats.free.Free
import cats.syntax.xor._
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.utils.XorCIO._
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.domain.{ Installation, SharedCollection, SharedCollectionSubscription }
import com.fortysevendeg.ninecards.services.persistence.{ SharedCollectionPersistenceServices, _ }
import doobie.imports._

import scalaz.concurrent.Task
import scalaz.syntax.applicative._

class SharedCollectionProcesses[F[_]](
  implicit
  collectionPersistence: SharedCollectionPersistenceServices,
  subscriptionPersistence: SharedCollectionSubscriptionPersistenceServices,
  transactor: Transactor[Task],
  dbOps: DBOps[F]
) {

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "The required shared collection doesn't exist"
  )

  def createCollection(request: CreateCollectionRequest): Free[F, CreateCollectionResponse] = {
    for {
      sharedCollection ← collectionPersistence.addCollection[SharedCollection](request.collection)
      response ← collectionPersistence.addPackages(sharedCollection.id, request.packages)
    } yield toCreateCollectionResponse(sharedCollection, request.packages)
  }.liftF[F]

  def getCollectionByPublicIdentifier(publicIdentifier: String): Free[F, XorGetCollectionByPublicId] = {
    def getPackages(collection: SharedCollection): ConnectionIO[GetCollectionByPublicIdentifierResponse] =
      for {
        packages ← collectionPersistence.getPackagesByCollection(collection.id)
      } yield toGetCollectionByPublicIdentifierResponse(collection, packages)

    val sh1: XorCIO[Throwable, SharedCollection] = findCollection(publicIdentifier)
    val sharedCollectionInfo: XorCIO[Throwable, GetCollectionByPublicIdentifierResponse] =
      flatMapXorCIO(sh1, getPackages)
    sharedCollectionInfo.liftF[F]
  }

  /**
    * This process changes the application state to one where the user is subscribed to the collection.
    *
    */
  def subscribe(publicIdentifier: String, userId: Long): Free[F, Xor[Throwable, SubscribeResponse]] = {

    // Now: if already subscribed, you should do nothing
    def addSubscription(collection: SharedCollection): ConnectionIO[SubscribeResponse] =
      for {
        oldOpt ← subscriptionPersistence.getSubscriptionByCollectionAndUser(collection.id, userId)
        _ ← oldOpt match {
          case Some(c) ⇒
            c.point[ConnectionIO]
          case None ⇒
            subscriptionPersistence.addSubscription[SharedCollectionSubscription](collection.id, userId)
        }
      } yield SubscribeResponse()

    val sh1: XorCIO[Throwable, SharedCollection] = findCollection(publicIdentifier)
    val subscriptionInfo: XorCIO[Throwable, SubscribeResponse] = flatMapXorCIO(sh1, addSubscription)

    subscriptionInfo.liftF[F]
  }

  private[this] def findCollection(publicId: String): XorCIO[Throwable, SharedCollection] =
    collectionPersistence
      .getCollectionByPublicIdentifier(publicId)
      .map(Xor.fromOption(_, sharedCollectionNotFoundException))

}

object SharedCollectionProcesses {

  implicit def sharedCollectionProcesses[F[_]](
    implicit
    sharedCollectionPersistenceServices: SharedCollectionPersistenceServices,
    dbOps: DBOps[F]
  ) = new SharedCollectionProcesses

}