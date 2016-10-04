package cards.nine.googleplay.util

import cards.nine.googleplay.domain._
import cards.nine.googleplay.api._
import cats.data.Xor
import enumeratum.{ Enum, EnumEntry }
import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._

object ScalaCheck {

  case class PathSegment(value: String)

  // The automatic generator would produce empty strings. We want non-empty ones.

  implicit val arbPathSegment: Arbitrary[PathSegment] =
    Arbitrary(ScalaCheck_Aux.genUriPathString.map(PathSegment.apply(_)))

  implicit val arbPackage: Arbitrary[Package] =
    Arbitrary(nonEmptyListOf(alphaNumChar).map(chars ⇒ Package(chars.mkString)))

  implicit val arbAuth: Arbitrary[GoogleAuthParams] =
    ScalaCheck_Aux.arbAuth

  implicit val arbFullCard: Arbitrary[FullCard] =
    ScalaCheck_Aux.arbFullCard

  implicit val arbApiCard: Arbitrary[ApiCard] =
    ScalaCheck_Aux.arbApiCard

  implicit val arbItem: Arbitrary[Item] =
    ScalaCheck_Aux.arbItem

  implicit val arbString: Arbitrary[String] =
    ScalaCheck_Aux.arbString

  implicit val arbGetCardAnswer: Arbitrary[Xor[InfoError, FullCard]] =
    ScalaCheck_Aux.arbGetCardAnswer

  implicit val arbResolveAnswer: Arbitrary[Xor[String, Item]] =
    ScalaCheck_Aux.arbResolveAnswer

  implicit val arbCategory: Arbitrary[Category] =
    ScalaCheck_Aux.arbCategory

  implicit val arbPriceFilter: Arbitrary[PriceFilter] =
    ScalaCheck_Aux.arbPriceFilter

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
  import org.scalacheck.Shapeless._

  // A generator of strings that served as non-interpreted parts of an URI (path, query param or value)
  val genUriPathString: Gen[String] = {
    // Unreserved characters, per URI syntax: https://tools.ietf.org/html/rfc2396#section-2.3
    val unreserved: Gen[Char] = Gen.frequency((9, Gen.alphaNumChar), (1, oneOf('-', '.', '_', '~')))
    Gen.containerOf[Array, Char](unreserved).map(_.mkString)
      .filter(path ⇒ !List(".", "..").contains(path))
  }

  val usAsciiStringGen = Gen.containerOf[Array, Char](Gen.choose[Char](0, 127)).map(_.mkString)

  val arbAuth = implicitly[Arbitrary[GoogleAuthParams]]

  val arbString = implicitly[Arbitrary[String]]

  def enumeratumGen[C <: EnumEntry](e: Enum[C]): Gen[C] =
    for (i ← Gen.choose(0, e.values.length - 1)) yield e.values(i)

  def enumeratumArbitrary[C <: EnumEntry](implicit e: Enum[C]): Arbitrary[C] =
    Arbitrary(enumeratumGen(e))

  val arbCategory: Arbitrary[Category] = enumeratumArbitrary[Category](Category)

  val arbPriceFilter: Arbitrary[PriceFilter] = enumeratumArbitrary[PriceFilter](PriceFilter)

  val genApiCard: Gen[ApiCard] =
    for /*scalacheck.Gen*/ {
      title ← identifier
      docid ← identifier
      appDetails ← listOf(identifier)
    } yield ApiCard(
      packageName = docid,
      title       = title,
      free        = false,
      icon        = "",
      stars       = 0.0,
      categories  = appDetails,
      downloads   = ""
    )

  val genFullCard: Gen[FullCard] =
    for /*ScalaCheck.Gen*/ {
      title ← identifier
      docid ← identifier
      appDetails ← listOf(identifier)
    } yield FullCard(
      packageName = docid,
      title       = title,
      free        = false,
      icon        = "",
      stars       = 0.0,
      categories  = appDetails,
      screenshots = List(),
      downloads   = ""
    )

  val arbApiCard: Arbitrary[ApiCard] = Arbitrary(genApiCard)

  val arbFullCard: Arbitrary[FullCard] = Arbitrary(genFullCard)

  val arbItem: Arbitrary[Item] = Arbitrary(for {
    title ← identifier
    docid ← identifier
    appDetails ← listOf(identifier)
  } yield Item(
    DocV2(
      title           = title,
      creator         = "",
      docid           = docid,
      details         = Details(
        appDetails = AppDetails(
          appCategory  = appDetails,
          numDownloads = "",
          permission   = Nil
        )
      ),
      aggregateRating = AggregateRating.Zero,
      image           = Nil,
      offer           = Nil
    )
  ))

  val arbGetCardAnswer = {
    implicit val app: Arbitrary[FullCard] = arbFullCard
    implicit val fail: Arbitrary[String] = arbString
    implicitly[Arbitrary[Xor[InfoError, FullCard]]]
  }

  val arbResolveAnswer: Arbitrary[Xor[String, Item]] = {
    implicit val item: Arbitrary[Item] = arbItem
    implicitly[Arbitrary[Xor[String, Item]]]
  }

}