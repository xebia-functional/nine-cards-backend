package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages._
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay

class ApplicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def getAppsInfo(
    packagesName: List[String],
    authParams: AuthParams
  ): Free[F, GetAppsInfoResponse] =
    if (packagesName.isEmpty)
      Free.pure[F, GetAppsInfoResponse](GetAppsInfoResponse(Nil, Nil))
    else
      services.resolveMany(
        packageNames = packagesName,
        auth         = toAuthParamsServices(authParams)
      ) map toGetAppsInfoResponse
}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new ApplicationProcesses

}
