package com.fortysevendeg.ninecards.services.free.domain.queries

import java.sql.Timestamp
import java.time.LocalDateTime

import com.fortysevendeg.ninecards.services.free.domain.SharedCollectionPackage.Queries._
import com.fortysevendeg.ninecards.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class SharedCollectionPackageQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val id = 12345l
  val collectionId = 23456l
  val packageName = "com.fortysevendeg.ninecards"

  val getByIdQuery = collectionPackagePersistence.generateQuery(
    sql    = getById,
    values = id
  )
  check(getByIdQuery)

  val getBySharedCollectionQuery = collectionPackagePersistence.generateQuery(
    sql    = getBySharedCollection,
    values = collectionId
  )
  check(getBySharedCollectionQuery)

  val insertCollectionQuery = collectionPackagePersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (collectionId, packageName)
  )
  check(insertCollectionQuery)

}
