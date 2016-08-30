package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages._
import com.fortysevendeg.ninecards.services.free.algebra.GooglePlay

class ApplicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) {

  def categorizeApps(
    packagesName: List[String],
    authParams: AuthParams
  ): Free[F, CategorizeAppsResponse] = packagesName match {
    case Nil ⇒ Free.pure[F, CategorizeAppsResponse](CategorizeAppsResponse(Nil, Nil))
    case _ ⇒
      services.resolveMany(
        packageNames = packagesName,
        auth         = toAuthParamsServices(authParams)
      ) map toCategorizeAppsResponse
  }
}

object ApplicationProcesses {

  implicit def applicationProcesses[F[_]](implicit services: GooglePlay.Services[F]) =
    new ApplicationProcesses

}
