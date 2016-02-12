package com.fortysevendeg.ninecards.api

import org.specs2.matcher.MatchResult
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.testkit.Specs2RouteTest
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import org.specs2.mutable.Specification
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import Domain._

import cats.data.Xor
import cats.syntax.option._
import com.akdeniz.googleplaycrawler.GooglePlayException

import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

class NineCardsGooglePlayApiSpec
    extends Specification
    with ScalaCheck
    with Specs2RouteTest {

  val requestHeaders = List(
    RawHeader("X-Android-ID", "androidId"),
    RawHeader("X-Google-Play-Token", "googlePlayToken"),
    RawHeader("X-Android-Market-Localization", "es-ES")
  )

  // we want a slightly different generator to the one that would be automatically generated - no empty string.
  implicit val arbPackage: Arbitrary[Package] = Arbitrary(nonEmptyListOf(alphaNumChar).map(chars => Package(chars.mkString)))

  // TODO pull this out somewhere else
  // A generator which returns a map of A->B, a list of As that are in the map, and a list of As that are not
  def genPick[A, B](implicit arba: Arbitrary[A], arbb: Arbitrary[B]): Gen[(Map[A, B], List[A], List[A])] = for {
    pairs <- arbitrary[Map[A, B]]
    keys = pairs.keySet
    validPicks <- someOf(keys)
    anotherList <- listOf(arbitrary[A])
    invalidPicks = anotherList.filterNot(i => keys.contains(i))
  } yield (pairs, validPicks.toList, invalidPicks)

  "Nine Cards Google Play Api" should {

    "give the package name for a known single Google Play Store app" in prop { (pkg: Package, item: Item) =>

      def requestPackage(t: Token, id: AndroidId, lo: Option[Localisation]): Package => Xor[GooglePlayException, Item] = { p =>
        if(p == pkg) {
          Xor.right(item)
        } else {
          Xor.left(new GooglePlayException("Looking for package that does not exist"))
        }
      }

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute(requestPackage _)

      Get(s"/googleplay/package/${pkg.value}") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_== OK
        val response = responseAs[String]
        decode[Item](response) must_=== Xor.right(item)
      }
    }

    "fail with an Internal Server Error when the package is not known" in prop { (unknownPackage: Package, wrongItem: Item) =>

      def requestPackage(t: Token, id: AndroidId, lo: Option[Localisation]): Package => Xor[GooglePlayException, Item] = { p =>
        if(p == unknownPackage) {
          Xor.left(new GooglePlayException("Package does not exist"))
        } else {
          Xor.right(wrongItem)
        }
      }

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute(requestPackage _)

      Get(s"/googleplay/package/${unknownPackage.value}") ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_=== InternalServerError
      }
    }

    "give the package details for the known packages and highlight the errors" in prop { (data: (Map[Package, Item], List[Package], List[Package])) =>

      val (database, succs, errs) = data

      val expectedResult = PackageDetails(
        errors = errs.map(_.value),
        items = succs.map(i => database(i))
      )

      def requestPackage(t: Token, id: AndroidId, lo: Option[Localisation]): Package => Xor[GooglePlayException, Item] = { p =>
        database.get(p).toRightXor[GooglePlayException](new GooglePlayException("Package ${p.value} does not exist"))
      }

      val route = new NineCardsGooglePlayApi {
        override def actorRefFactory = system
      }.googlePlayApiRoute(requestPackage _)

      val allPackages = (succs ++ errs).map(_.value)

      Post("/googleplay/packages/detailed", PackageListRequest(allPackages).asJson.noSpaces) ~> addHeaders(requestHeaders) ~> route ~> check {
        status must_== OK
        val response = responseAs[String]
        val decoded = decode[PackageDetails](response).getOrElse(throw new RuntimeException(s"Unable to parse response [$response]"))

        decoded.errors aka "Expected error items" must containTheSameElementsAs(expectedResult.errors)
        decoded.items aka "Expected successful items" must containTheSameElementsAs(expectedResult.items)
      }
    }.setGen(genPick[Package, Item])
  }
}
