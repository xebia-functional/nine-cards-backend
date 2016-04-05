package com.fortysevendeg.ninecards.processes

import cats.data.Xor
import cats.free.Free
import cats.syntax.xor._
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.services.common.TaskOps._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.domain.{Installation, SharedCollection}
import com.fortysevendeg.ninecards.services.persistence.{SharedCollectionPersistenceServices, _}
import doobie.imports._

import scalaz.concurrent.Task
import scalaz.syntax.applicative._

class SharedCollectionProcesses[F[_]](
    implicit
    sharedCollectionPersistenceServices: SharedCollectionPersistenceServices,
    transactor:                          Transactor[Task],
    dbOps:                               DBOps[F]
) {

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "The required shared collection doesn't exist"
  )

  def getCollectionByPublicIdentifier(
    publicIdentifier: String
  )(
    implicit
    ev: Composite[Installation]
  ): Free[F, XorGetCollectionByPublicId] = {

    val sharedCollectionInfo = for {
      sharedCollection ← sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
        publicIdentifier = publicIdentifier
      )
      response ← getPackagesByCollection(sharedCollection)
    } yield response

    sharedCollectionInfo.liftF[F]
  }

  private[this] def getPackagesByCollection(collection: Option[SharedCollection]) = {
    val throwable: XorGetCollectionByPublicId = sharedCollectionNotFoundException.left

    collection.fold(throwable.point[ConnectionIO]) { c ⇒
      sharedCollectionPersistenceServices.getPackagesByCollection(c.id) map { packages ⇒
        toGetCollectionByPublicIdentifierResponse(c, packages).right
      }
    }
  }
}

object SharedCollectionProcesses {

  implicit def sharedCollectionProcesses[F[_]](
    implicit
    sharedCollectionPersistenceServices: SharedCollectionPersistenceServices,
    dbOps:                               DBOps[F]
  ) = new SharedCollectionProcesses

}