package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.GooglePlayMessages._
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay

class GooglePlayProcesses[F[_]](implicit googlePlayServices: GooglePlay.Services[F]) {

  def categorizeApps(
    packagesName: List[String],
    authParams: AuthParams
  ): Free[F, CategorizeAppsResponse] =
    googlePlayServices.resolveMany(
      packageNames = packagesName,
      auth         = toAuthParamsServices(authParams)
    ) map toCategorizeAppsResponse
}

object GooglePlayProcesses {

  implicit def googlePlayProcesses[F[_]](implicit googlePlayServices: GooglePlay.Services[F]) =
    new GooglePlayProcesses

}
