package com.fortysevendeg.ninecards.googleplay.util

import com.fortysevendeg.ninecards.googleplay.domain._
import cats.data.Xor
import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._

object ScalaCheck {

  // The automatic generator would produce empty strings. We want non-empty ones.

  implicit val arbPackage: Arbitrary[Package] =
    Arbitrary(nonEmptyListOf(alphaNumChar).map(chars => Package(chars.mkString)))

  implicit val arbAuth: Arbitrary[GoogleAuthParams] =
    ScalaCheck_Aux.arbAuth

  implicit val arbAppCard: Arbitrary[AppCard] =
    ScalaCheck_Aux.arbAppCard

  implicit val arbItem: Arbitrary[Item] =
    ScalaCheck_Aux.arbItem

  implicit val arbString: Arbitrary[String] =
    ScalaCheck_Aux.arbString

  implicit val arbGetCardAnswer: Arbitrary[Xor[String,AppCard]] =
    ScalaCheck_Aux.arbGetCardAnswer

  implicit val arbResolveAnswer: Arbitrary[Xor[String,Item]] =
    ScalaCheck_Aux.arbResolveAnswer

  // TODO pull this out somewhere else
  // A generator which returns a map of A->B, a list of As that are in the map, and a list of As that are not
  def genPick[A, B](implicit aa: Arbitrary[A], ab: Arbitrary[B]): Gen[(Map[A, B], List[A], List[A])] = for {
    pairs <- arbitrary[Map[A, B]]
    keys = pairs.keySet
    validPicks <- someOf(keys)
    anotherList <- listOf(arbitrary[A])
    invalidPicks = anotherList.filterNot(i => keys.contains(i))
  } yield (pairs, validPicks.toList, invalidPicks)

}

object ScalaCheck_Aux {
  import org.scalacheck.Shapeless._

  val arbAuth = implicitly[Arbitrary[GoogleAuthParams]]

  val arbString = implicitly[Arbitrary[String]]

  val arbAppCard: Arbitrary[AppCard] = Arbitrary(for {
    title <- identifier
    docid <- identifier
    appDetails <- listOf(identifier)
  } yield AppCard(
    packageName   = docid,
    title   = title,
    free = false,
    icon = "",
    stars = 0.0,
    categories  = appDetails,
    downloads = ""
  )
  )

  val arbItem: Arbitrary[Item] = Arbitrary(for {
    title <- identifier
    docid <- identifier
    appDetails <- listOf(identifier)
  } yield Item(
    DocV2(
      title   = title,
      creator = "",
      docid   = docid,
      details = Details(
        appDetails = AppDetails(
          appCategory  = appDetails,
          numDownloads = "",
          permission   = Nil
        )
      ),
      aggregateRating = AggregateRating.Zero,
      image = Nil,
      offer = Nil
    )
  ))

  val arbGetCardAnswer = {
    implicit val app: Arbitrary[AppCard] = arbAppCard
    implicit val fail: Arbitrary[String] = arbString
    implicitly[Arbitrary[Xor[String,AppCard]]]
  }

  val arbResolveAnswer: Arbitrary[Xor[String,Item]] = {
    implicit val item: Arbitrary[Item] = arbItem
    implicitly[Arbitrary[Xor[String,Item]]]
  }

}