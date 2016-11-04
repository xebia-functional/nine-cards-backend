package cards.nine.googleplay.util

import cards.nine.domain.application.{ BasicCard, FullCard }
import cards.nine.domain.ScalaCheck.arbPackage
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cats.data.Xor
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.Shapeless._
import org.scalacheck._

object ScalaCheck {

  case class PathSegment(value: String)

  // The automatic generator would produce empty strings. We want non-empty ones.

  implicit val arbPathSegment: Arbitrary[PathSegment] =
    Arbitrary(ScalaCheck_Aux.genUriPathString.map(PathSegment.apply))

  implicit val arbAuth: Arbitrary[MarketCredentials] =
    ScalaCheck_Aux.arbAuth

  implicit val arbFullCard: Arbitrary[FullCard] =
    ScalaCheck_Aux.arbFullCard

  implicit val arbGetCardAnswer: Arbitrary[Xor[InfoError, FullCard]] =
    ScalaCheck_Aux.arbGetCardAnswer

  // TODO pull this out somewhere else
  // A generator which returns a map of A->B, a list of As that are in the map, and a list of As that are not
  def genPick[A, B](implicit aa: Arbitrary[A], ab: Arbitrary[B]): Gen[(Map[A, B], List[A], List[A])] = for {
    pairs ← arbitrary[Map[A, B]]
    keys = pairs.keySet
    validPicks ← someOf(keys)
    anotherList ← listOf(arbitrary[A])
    invalidPicks = anotherList.filterNot(i ⇒ keys.contains(i))
  } yield (pairs, validPicks.toList, invalidPicks)

}

object ScalaCheck_Aux {

  // A generator of strings that served as non-interpreted parts of an URI (path, query param or value)
  val genUriPathString: Gen[String] = {
    // Unreserved characters, per URI syntax: https://tools.ietf.org/html/rfc2396#section-2.3
    val unreserved: Gen[Char] = Gen.frequency((9, Gen.alphaNumChar), (1, oneOf('-', '.', '_', '~')))
    Gen.containerOf[Array, Char](unreserved).map(_.mkString)
      .filter(path ⇒ !List(".", "..").contains(path))
  }

  val usAsciiStringGen = Gen.containerOf[Array, Char](Gen.choose[Char](0, 127)).map(_.mkString)

  val arbAuth = implicitly[Arbitrary[MarketCredentials]]

  val genFullCard: Gen[FullCard] =
    for /*ScalaCheck.Gen*/ {
      title ← identifier
      packageName ← arbPackage.arbitrary
      appDetails ← listOf(identifier)
    } yield FullCard(
      packageName = packageName,
      title       = title,
      free        = false,
      icon        = "",
      stars       = 0.0,
      categories  = appDetails,
      screenshots = List(),
      downloads   = ""
    )

  val genBasicCard: Gen[BasicCard] = genFullCard.map(_.toBasic)

  val arbFullCard: Arbitrary[FullCard] = Arbitrary(genFullCard)

  val arbGetCardAnswer = {
    implicit val app: Arbitrary[FullCard] = arbFullCard
    implicitly[Arbitrary[Xor[InfoError, FullCard]]]
  }

}