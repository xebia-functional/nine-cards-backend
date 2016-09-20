package cards.nine.processes

import cats.free.Free
import cards.nine.processes.converters.Converters._
import cards.nine.processes.messages.ApplicationMessages._
import cards.nine.processes.messages.GooglePlayAuthMessages._
import cards.nine.services.free.algebra.GooglePlay

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
