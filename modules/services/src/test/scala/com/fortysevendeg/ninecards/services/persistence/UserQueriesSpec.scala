package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{User}
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class UserQueriesSpec
  extends Specification
    with AnalysisSpec
    with DomainDatabaseContext {

  val getUserByEmailQuery = persistenceImpl.generateQuery[String, User](User.Queries.getByEmail, "hello@47deg.com")
  check(getUserByEmailQuery)

  val insertUser = persistenceImpl.generateUpdateWithGeneratedKeys[(String, String), User](User.Queries.insert, ("hello@47deg.com", "e1e938889-2e2d-49d7-81e7-10606c4ca32f"))
  check(insertUser)

}
