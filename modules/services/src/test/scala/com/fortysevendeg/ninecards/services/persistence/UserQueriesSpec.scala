package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.User.Queries._
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class UserQueriesSpec
  extends Specification
    with AnalysisSpec
    with DomainDatabaseContext {

  val getUserByEmailQuery = userPersistence.generateQuery(
    sql = getByEmail,
    values = "hello@47deg.com")
  check(getUserByEmailQuery)

  val insertUserQuery = userPersistence.generateUpdateWithGeneratedKeys(
    sql = insert,
    values = ("hello@47deg.com", "e1e938889-2e2d-49d7-81e7-10606c4ca32f", "7de44327-2e19-4a7f-b02f-cdc0c9d7c21c"))
  check(insertUserQuery)

}
