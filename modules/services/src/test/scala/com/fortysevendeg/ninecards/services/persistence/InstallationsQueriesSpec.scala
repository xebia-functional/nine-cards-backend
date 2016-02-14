package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.Installation
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class InstallationsQueriesSpec
  extends Specification
    with AnalysisSpec
    with DomainDatabaseContext {

  val getByUserAndAndroidId = persistenceImpl.generateQuery[(Long, String), Installation](Installation.Queries.getByUserAndAndroidId, (1, "111a-222b-33c-444d13"))
  check(getByUserAndAndroidId)

  val insertInstallation = persistenceImpl.generateUpdateWithGeneratedKeys[(Long, Option[String], String), Installation](Installation.Queries.insert, (1, Option("111a-222b-4d13"), "35a4df64a31adf3"))
  check(insertInstallation)

  val updateDeviceToken = persistenceImpl.generateUpdateWithGeneratedKeys[(Option[String], Long, String), Installation](Installation.Queries.updateDeviceToken, (Option("111a-222b-4d13"), 1, "35a4df64a31adf3"))
  check(updateDeviceToken)

}