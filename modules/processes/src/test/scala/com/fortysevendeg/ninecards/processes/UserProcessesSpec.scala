package com.fortysevendeg.ninecards.processes

import cats.Monad
import cats.free.Free
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages.{UpdateInstallationResponse, UpdateInstallationRequest}
import com.fortysevendeg.ninecards.processes.messages.UserMessages.{LoginResponse, LoginRequest}
import com.fortysevendeg.ninecards.services.free.domain.{Installation, User}
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Shapeless._
import org.specs2.matcher.Matchers
import org.mockito.Matchers.{eq => mockEq}
import org.specs2.mock.Mockito
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

    implicit val userPersistenceServices: UserPersistenceServices = mock[UserPersistenceServices]
    implicit val userProcesses = new UserProcesses[NineCardsServices]

  }

  trait SuccessfulScope extends BasicScope {

    userPersistenceServices.getUserByEmail(email) returns Option(user).point[ConnectionIO]

    userPersistenceServices.getInstallationByUserAndAndroidId(userId,androidId) returns Option(installation).point[ConnectionIO]

    userPersistenceServices.addUser[User](email, sessionToken) returns user.point[ConnectionIO]

    userPersistenceServices.updateInstallation[Installation](mockEq(userId), mockEq(Option(deviceToken)), mockEq(androidId))(any) returns installation.point[ConnectionIO]


  }

  trait UnsuccessfulScope extends BasicScope {
    val user: Option[User] = None
    userPersistenceServices.getUserByEmail(email) returns user.point[ConnectionIO]
  }

  trait FailingScope extends BasicScope {

  }

}

trait UserProcessesContext {

  val email = "valid.email@test.com"

  val userId = 1l

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val banned = false

  val user: User = User(userId, email, sessionToken, banned)

  val androidId = "f07a13984f6d116a"

  val outhToken = "hd-w2tmEe7SZ_8vXhw_3f1iNnsrAqkpEvbPkFIo9oZeAq26u"

  val deviceToken = "abc"

  val loginRequest = LoginRequest(email, androidId, outhToken)

  val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, Option(deviceToken))

  val updateInstallationResponse = UpdateInstallationResponse(androidId, Option(deviceToken))

  val expectedInstallationResponse = Free.pure(updateInstallationResponse)

  val installation = Installation(1l,userId,Option(deviceToken),androidId)

}


class UserProcessesSpec
  extends UserProcessesSpecification
    with ScalaCheck {

//  "signUpUser" should {
//    "return LoginResponse object" in new SuccessfulScope {
//      val signUpUser: Free[NineCardsServices, LoginResponse] = userProcesses.signUpUser(loginRequest)
//      println(s"###@@@@@#@#@#@#@#@#@#@#@#@#@#@#$signUpUser")
//
//      1 shouldEqual 1
//    }
//  }

  "updateInstallation" should {
    "return UpdateInstallationResponse object" in new SuccessfulScope {
      val signUpInstallation = userProcesses.updateInstallation(updateInstallationRequest)
      signUpInstallation.foldMap(interpreters).run shouldEqual updateInstallationResponse
    }
  }

}
