package com.fortysevendeg.ninecards.services.free.domain.queries

import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionSubscription.Queries._
import com.fortysevendeg.ninecards.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class SharedCollectionSubscriptionQueriesSpec
    extends Specification
    with AnalysisSpec
    with DomainDatabaseContext {

  val collectionId = 12345l
  val id = 23456l
  val userId = 34567l

  val getSubscriptionByIdQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getById,
    values = id
  )
  check(getSubscriptionByIdQuery)

  val getByCollectionQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollection,
    values = collectionId
  )
  check(getByCollectionQuery)

  val getByCollectionAndUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(getByCollectionAndUserQuery)

  val getByUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByUser,
    values = userId
  )
  check(getByUserQuery)

  val insertQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (collectionId, userId)
  )
  check(insertQuery)

  val deleteQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = delete,
    values = id
  )
  check(deleteQuery)

  val deleteByCollectionAndUserQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = deleteByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(deleteByCollectionAndUserQuery)
}
