package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.SharedCollection

class SharedCollectionPersistenceImpl {

  def addSharedCollection(sharedCollection: SharedCollection): SharedCollection = sharedCollection

  def getSharedCollectionById(sharedCollectionId: String): Option[SharedCollection] =
    Option(
      SharedCollection(
        sharedCollectionId = None,
        name = "Collection",
        resolvedPackages = Seq.empty))

  def updateInstallNotification(sharedCollectionId: String): SharedCollection =
    SharedCollection(
      sharedCollectionId = None,
      name = "Collection",
      resolvedPackages = Seq.empty)

  def updateViewNotification(sharedCollectionId: String): SharedCollection =
    SharedCollection(
      sharedCollectionId = None,
      name = "Collection",
      resolvedPackages = Seq.empty)
}

object SharedCollectionPersistenceImpl {

  implicit def sharedCollectionPersistenceImpl = new SharedCollectionPersistenceImpl()
}
