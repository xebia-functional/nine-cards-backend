package com.fortysevendeg.ninecards.processes

import cats.free.Free
import com.fortysevendeg.ninecards.processes.domain.{Installation, User}
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.services.free.algebra.Users._

import scala.language.higherKinds

class UserProcesses[F[_]](
  implicit userServices: UserServices[F]) {

  def getUserById(userId: String): Free[F, User] = for {
    persistenceApps <- userServices.getUserById(userId)
  } yield (persistenceApps map toUserApp).getOrElse(throw new RuntimeException(""))

  def createInstallation(request: InstallationRequest): Free[F, Installation] = for {
    newInstallation <- userServices.createInstallation(toInstallationRequestProcess(request))
  } yield fromInstallationProcesses(newInstallation)

  def updateInstallation(installationId: String, request: InstallationRequest): Free[F, Installation] = for {
    updateInstallation <- userServices.updateInstallation(toInstallationRequestProcess(request), installationId)
  } yield fromInstallationProcesses(updateInstallation)

}

object UserProcesses {

  implicit def userProcesses[F[_]](implicit userServices: UserServices[F]) = new UserProcesses()

}
