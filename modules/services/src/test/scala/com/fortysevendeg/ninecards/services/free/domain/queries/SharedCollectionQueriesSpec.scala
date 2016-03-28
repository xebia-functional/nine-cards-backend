package com.fortysevendeg.ninecards.services.free.domain.queries

import java.sql.Timestamp
import java.time.LocalDateTime

import com.fortysevendeg.ninecards.services.common.TupleGeneric
import com.fortysevendeg.ninecards.services.free.domain.SharedCollection.Queries._
import com.fortysevendeg.ninecards.services.persistence.DomainDatabaseContext
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.SharedCollectionData
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class SharedCollectionQueriesSpec
  extends Specification
    with AnalysisSpec
    with DomainDatabaseContext {

  val id = 12345l
  val publicIdentifier = "7a2a4c1c-5260-40a5-ba06-db009a3ef7c4"
  val now = Timestamp.valueOf(LocalDateTime.now)

  val data = SharedCollectionData(
    publicIdentifier = publicIdentifier,
    userId = Option(23456l),
    publishedOn = now,
    description = Option("Description about the collection"),
    author = "John Doe",
    name = "The name of the collection",
    installations = 1,
    views = 1,
    category = "SOCIAL",
    icon = "path-to-icon",
    community = true)

  val getCollectionByIdQuery = collectionPersistence.generateQuery(
    sql = getById,
    values = id)
  check(getCollectionByIdQuery)

  val getPublicIdentifierQuery = collectionPersistence.generateQuery(
    sql = getByPublicIdentifier,
    values = publicIdentifier)
  check(getPublicIdentifierQuery)

  val insertCollectionQuery = collectionPersistence.generateUpdateWithGeneratedKeys(
    sql = insert,
    values = TupleGeneric[SharedCollectionData].to(data))
  check(insertCollectionQuery)

}
