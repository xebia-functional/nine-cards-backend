package com.fortysevendeg.ninecards.processes

import cats.Monad
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.{LoginRequest, LoginResponse}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices
import doobie.imports._
import org.mockito.Matchers.{eq => mockEq}
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.Scalaz._
import scalaz.concurrent.Task


trait UserProcessesSpecification
  extends Specification
    with Matchers
    with Mockito
    with UserProcessesContext {

  implicit def taskMonad = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
      fa.flatMap(f)

    override def pure[A](a: A): Task[A] = Task.now(a)
  }

  trait BasicScope extends Scope {

    implicit val transactor = DriverManagerTransactor[Task](
      driver = "org.h2.Driver",
      url = "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1",
      user = "sa",
      pass = "")
    implicit val userPersistenceServices: UserPersistenceServices = mock[UserPersistenceServices]
    implicit val userProcesses = new UserProcesses[NineCardsServices]

  }

  trait UserAndInstallationSucessfulScope extends BasicScope {

    userPersistenceServices.getUserByEmail(mockEq(email)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns Option(installation).point[ConnectionIO]

    userPersistenceServices.updateInstallation[Installation](mockEq(userId), mockEq(Option(deviceToken)), mockEq(androidId))(any) returns installation.point[ConnectionIO]

    userPersistenceServices.getUserBySessionToken(mockEq(sessionToken)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns Option(installation).point[ConnectionIO]
  }

  trait UserSucessfulAndInstallationFailingScope extends BasicScope {

    userPersistenceServices.getUserByEmail(mockEq(email)) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(mockEq(userId), mockEq(androidId)) returns nonExistingInstallation.point[ConnectionIO]

    userPersistenceServices.createInstallation[Installation](mockEq(userId), mockEq(None), mockEq(androidId))(any) returns installation.point[ConnectionIO]
  }

  trait UserAndInstallationFailingScope extends BasicScope {

    userPersistenceServices.getUserByEmail(email) returns nonExistingUser.point[ConnectionIO]

    userPersistenceServices.addUser[User](mockEq(email), any[String], any[String])(any) returns user.point[ConnectionIO]

    userPersistenceServices.createInstallation[Installation](mockEq(userId), mockEq(None), mockEq(androidId))(any) returns installation.point[ConnectionIO]

    userPersistenceServices.getUserBySessionToken(mockEq(sessionToken)) returns nonExistingUser.point[ConnectionIO]
  }

}

trait UserProcessesContext {

  val email = "valid.email@test.com"

  val userId = 1l

  val apiKey = "60b32e59-0d87-4705-a454-2e5b38bec13b"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val banned = false

  val user = User(userId, email, sessionToken, apiKey, banned)

  val nonExistingUser: Option[User] = None

  val androidId = "f07a13984f6d116a"

  val outhToken = "hd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26u"

  val deviceToken = "abc"

  val installationId = 1l

  val loginRequest = LoginRequest(email, androidId, outhToken)

  val loginResponse = LoginResponse(apiKey, sessionToken)

  val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, Option(deviceToken))

  val updateInstallationResponse = UpdateInstallationResponse(androidId, Option(deviceToken))

  val installation = Installation(installationId, userId, Option(deviceToken), androidId)

  val nonExistingInstallation: Option[Installation] = None

  val checkSessionTokenResponse = Option(userId)

}


class UserProcessesSpec
  extends UserProcessesSpecification
    with ScalaCheck {

  "signUpUser" should {
    "return LoginResponse object when the user exists and installation" in new UserAndInstallationSucessfulScope {
      val signUpUser = userProcesses.signUpUser(loginRequest)
      signUpUser.foldMap(interpreters).run shouldEqual loginResponse
    }

    "return LoginResponse object when the user exists but not installation" in new UserSucessfulAndInstallationFailingScope {
      val signUpUser = userProcesses.signUpUser(loginRequest)
      signUpUser.foldMap(interpreters).run shouldEqual loginResponse
    }

    "return LoginResponse object when there isn't user or installation" in new UserAndInstallationFailingScope {
      val signUpUser = userProcesses.signUpUser(loginRequest)
      signUpUser.foldMap(interpreters).run shouldEqual loginResponse
    }
  }

  "updateInstallation" should {
    "return UpdateInstallationResponse object" in new UserAndInstallationSucessfulScope {
      val signUpInstallation = userProcesses.updateInstallation(updateInstallationRequest)
      signUpInstallation.foldMap(interpreters).run shouldEqual updateInstallationResponse
    }
  }

  "checkSessionToken" should {
    "return the userId if exists a user with that sessionToken and androidId" in new UserAndInstallationSucessfulScope {
      val checkSessionToken = userProcesses.checkSessionToken(sessionToken, androidId)
      checkSessionToken.foldMap(interpreters).run shouldEqual checkSessionTokenResponse
    }
    "return None if not exists a user with that sessionToken and androidId" in new UserAndInstallationFailingScope {
      val checkSessionToken = userProcesses.checkSessionToken(sessionToken, androidId)
      checkSessionToken.foldMap(interpreters).run should beNone
    }
  }
}
