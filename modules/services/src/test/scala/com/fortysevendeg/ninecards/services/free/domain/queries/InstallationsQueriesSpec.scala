package com.fortysevendeg.ninecards.services.free.domain.queries

import com.fortysevendeg.ninecards.services.free.domain.Installation.Queries._
import com.fortysevendeg.ninecards.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class InstallationsQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val getByIdQuery = installationPersistence.generateQuery(
    sql    = getById,
    values = 1l
  )
  check(getByIdQuery)

  val getByUserAndAndroidIdQuery = installationPersistence.generateQuery(
    sql    = getByUserAndAndroidId,
    values = (1l, "111a-222b-33c-444d13")
  )
  check(getByUserAndAndroidIdQuery)

  val insertInstallationQuery = installationPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (1l, Option("111a-222b-4d13"), "35a4df64a31adf3")
  )
  check(insertInstallationQuery)

  val updateDeviceTokenQuery = installationPersistence.generateUpdateWithGeneratedKeys(
    sql    = updateDeviceToken,
    values = (Option("111a-222b-4d13"), 1l, "35a4df64a31adf3")
  )
  check(updateDeviceTokenQuery)

}