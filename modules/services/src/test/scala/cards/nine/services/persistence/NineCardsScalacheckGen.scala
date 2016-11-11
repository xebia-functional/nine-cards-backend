package cards.nine.services.persistence

import cards.nine.domain.account._
import cards.nine.domain.analytics._
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData
import cards.nine.services.free.interpreter.user.Services.UserData
import cards.nine.services.persistence.NineCardsGenEntities._
import cats.Monad
import java.sql.Timestamp
import java.time.Instant

import org.scalacheck.{ Arbitrary, Gen }

object NineCardsGenEntities {

  case class PublicIdentifier(value: String) extends AnyVal

  case class WrongIsoCode2(value: String) extends AnyVal

  case class CollectionTitle(value: String) extends AnyVal
}

trait NineCardsScalacheckGen {

  val timestampGenerator: Gen[Timestamp] = Gen.choose(0l, 253402300799l) map { seconds ⇒
    Timestamp.from(Instant.ofEpochSecond(seconds))
  }

  val sharedCollectionDataGenerator: Gen[SharedCollectionData] = for {
    publicIdentifier ← Gen.uuid
    publishedOn ← timestampGenerator
    author ← Gen.alphaStr
    name ← Gen.alphaStr
    installations ← Gen.posNum[Int]
    views ← Gen.posNum[Int]
    category ← nonEmptyString(64)
    icon ← nonEmptyString(64)
    community ← Gen.oneOf(true, false)
    packages ← Gen.listOf(arbPackage.arbitrary)
  } yield SharedCollectionData(
    publicIdentifier = publicIdentifier.toString,
    userId           = None,
    publishedOn      = publishedOn,
    author           = author,
    name             = name,
    installations    = installations,
    views            = views,
    category         = category,
    icon             = icon,
    community        = community,
    packages         = packages map (_.value)
  )

  val userDataGenerator: Gen[UserData] = for {
    email ← emailGenerator
    apiKey ← Gen.uuid
    sessionToken ← Gen.uuid
  } yield UserData(email.value, apiKey.toString, sessionToken.toString)

  implicit val abAndroidId: Arbitrary[AndroidId] = Arbitrary(Gen.uuid.map(u ⇒ AndroidId(u.toString)))

  implicit val abApiKey: Arbitrary[ApiKey] = Arbitrary(Gen.uuid.map(u ⇒ ApiKey(u.toString)))

  implicit val abCollectionTitle: Arbitrary[CollectionTitle] = Arbitrary(Gen.alphaStr.map(CollectionTitle))

  implicit val abDeviceToken: Arbitrary[DeviceToken] = Arbitrary(Gen.uuid.map(u ⇒ DeviceToken(u.toString)))

  implicit val abPublicIdentifier: Arbitrary[PublicIdentifier] = Arbitrary(Gen.uuid.map(u ⇒ PublicIdentifier(u.toString)))

  implicit val abSessionToken: Arbitrary[SessionToken] = Arbitrary(Gen.uuid.map(u ⇒ SessionToken(u.toString)))

  implicit val abSharedCollectionData: Arbitrary[SharedCollectionData] = Arbitrary(sharedCollectionDataGenerator)

  implicit val abUserData: Arbitrary[UserData] = Arbitrary(userDataGenerator)

  implicit val abWrongIsoCode2: Arbitrary[WrongIsoCode2] = Arbitrary(fixedLengthNumericString(2).map(WrongIsoCode2.apply))

  private[this] val genMonad: Monad[Gen] = new Monad[Gen] {
    def pure[A](a: A): Gen[A] = Gen.const(a)
    def flatMap[A, B](fa: Gen[A])(f: A ⇒ Gen[B]): Gen[B] = fa flatMap f
    override def tailRecM[A, B](a: A)(f: (A) ⇒ Gen[Either[A, B]]): Gen[B] =
      flatMap(f(a)) {
        case Right(b) ⇒ pure(b)
        case Left(nextA) ⇒ tailRecM(nextA)(f)
      }
  }

  private[this] def listOfDistinctN[A](min: Int, max: Int, gen: Gen[A]): Gen[List[A]] =
    for {
      num ← Gen.choose(min, max)
      elems ← Gen.listOfN(num, gen)
    } yield elems.distinct

  val genDeviceApp: Gen[UnrankedApp] = for {
    p ← arbPackage.arbitrary
    c ← arbCategory.arbitrary
  } yield UnrankedApp(p, c.entryName)

  implicit val abDeviceApp: Arbitrary[UnrankedApp] = Arbitrary(genDeviceApp)

}
