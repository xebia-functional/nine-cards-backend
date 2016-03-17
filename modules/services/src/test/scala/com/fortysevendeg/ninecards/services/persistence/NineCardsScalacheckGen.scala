package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities._
import org.scalacheck.{Arbitrary, Gen}

object NineCardsGenEntities {

  case class ApiKey(value: String) extends AnyVal

  case class Email(value: String) extends AnyVal

  case class SessionToken(value: String) extends AnyVal

  case class AndroidId(value: String) extends AnyVal

}

trait NineCardsScalacheckGen {

  def nonEmptyString(maxSize: Int) =
    Gen.resize(
      s = maxSize,
      g = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

  def fixedLengthString(size: Int) = Gen.listOfN(size, Gen.alphaChar).map(_.mkString)

  val emailGenerator: Gen[String] = for {
    mailbox <- nonEmptyString(50)
    topLevelDomain <- nonEmptyString(45)
    domain <- fixedLengthString(3)
  } yield s"$mailbox@$topLevelDomain.$domain"

  val uuidGenerator: Gen[String] = Gen.uuid.map(_.toString)

  implicit def abAndroidId: Arbitrary[AndroidId] = Arbitrary(uuidGenerator.map(AndroidId.apply))

  implicit def abApiKey: Arbitrary[ApiKey] = Arbitrary(uuidGenerator.map(ApiKey.apply))

  implicit def abEmail: Arbitrary[Email] = Arbitrary(emailGenerator.map(Email.apply))

  implicit def abSessionToken: Arbitrary[SessionToken] = Arbitrary(uuidGenerator.map(SessionToken.apply))
}
