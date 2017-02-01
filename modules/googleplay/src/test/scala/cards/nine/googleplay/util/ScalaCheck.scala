/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.util

import cards.nine.domain.application.{ BasicCard, FullCard }
import cards.nine.domain.ScalaCheck.arbPackage
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
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

  implicit val arbGetCardAnswer: Arbitrary[InfoError Either FullCard] =
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
    for {
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
    implicitly[Arbitrary[InfoError Either FullCard]]
  }

}