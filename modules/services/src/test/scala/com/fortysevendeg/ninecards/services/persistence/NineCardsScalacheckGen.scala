package com.fortysevendeg.ninecards.services.persistence

import java.sql.Timestamp
import java.time.Instant

import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices._
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices.UserData
import org.scalacheck.{ Arbitrary, Gen }

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
      g = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
    )

  def fixedLengthString(size: Int) = Gen.listOfN(size, Gen.alphaChar).map(_.mkString)

  val stringGenerator = Arbitrary.arbitrary[String]

  val emailGenerator: Gen[String] = for {
    mailbox ← nonEmptyString(50)
    topLevelDomain ← nonEmptyString(45)
    domain ← fixedLengthString(3)
  } yield s"$mailbox@$topLevelDomain.$domain"

  val timestampGenerator: Gen[Timestamp] = Gen.choose(0l, 253402300799l) map { seconds ⇒
    Timestamp.from(Instant.ofEpochSecond(seconds))
  }

  val sharedCollectionDataGenerator: Gen[SharedCollectionData] = for {
    publicIdentifier ← Gen.uuid
    publishedOn ← timestampGenerator
    description ← Gen.option[String](stringGenerator)
    author ← stringGenerator
    name ← stringGenerator
    installations ← Gen.posNum[Int]
    views ← Gen.posNum[Int]
    category ← stringGenerator
    icon ← stringGenerator
    community ← Gen.oneOf(true, false)
  } yield SharedCollectionData(
    publicIdentifier = publicIdentifier.toString,
    userId           = None,
    publishedOn      = publishedOn,
    description      = description,
    author           = author,
    name             = name,
    installations    = installations,
    views            = views,
    category         = category,
    icon             = icon,
    community        = community
  )

  val userDataGenerator: Gen[UserData] = for {
    email ← emailGenerator
    apiKey ← Gen.uuid
    sessionToken ← Gen.uuid
  } yield UserData(email, apiKey.toString, sessionToken.toString)

  implicit val abAndroidId: Arbitrary[AndroidId] = Arbitrary(Gen.uuid.map(u ⇒ AndroidId(u.toString)))

  implicit val abApiKey: Arbitrary[ApiKey] = Arbitrary(Gen.uuid.map(u ⇒ ApiKey(u.toString)))

  implicit val abEmail: Arbitrary[Email] = Arbitrary(emailGenerator.map(Email.apply))

  implicit val abSessionToken: Arbitrary[SessionToken] = Arbitrary(Gen.uuid.map(u ⇒ SessionToken(u.toString)))

  implicit val abSharedCollectionData: Arbitrary[SharedCollectionData] = Arbitrary(sharedCollectionDataGenerator)

  implicit val abUserData: Arbitrary[UserData] = Arbitrary(userDataGenerator)
}
