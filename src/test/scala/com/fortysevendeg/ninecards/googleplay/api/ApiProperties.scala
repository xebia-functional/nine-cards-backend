package com.fortysevendeg.ninecards.googleplay.api

import cats.{ Id, ~> }
import cats.data.Xor
import cats.syntax.option._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import io.circe.generic.auto._
import io.circe.parser._
import org.scalacheck.Prop._
import org.scalacheck._
import scalaz.concurrent.Task
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes._
import spray.testkit.{RouteTest, TestFrameworkInterface}

trait ScalaCheckRouteTest extends RouteTest with TestFrameworkInterface {
  def failTest(msg: String): Nothing = throw new RuntimeException(msg)
}

object ApiProperties extends Properties("Nine Cards Google Play API") with ScalaCheckRouteTest {

  import NineCardsMarshallers._

  val requestHeaders = List(
    RawHeader(Headers.androidId, "androidId"),
    RawHeader(Headers.token, "googlePlayToken"),
    RawHeader(Headers.localization, "es-ES")
  )

  import com.fortysevendeg.ninecards.googleplay.util.ScalaCheck._

  def resolveOne(pkg: Package) =
    Get(s"/googleplay/package/${pkg.value}") ~> addHeaders(requestHeaders)

  def resolveOneInterpreter(fun: Package => Option[Item]): Ops ~> Id = new (Ops ~> Id){
    def apply[A](fa: Ops[A]) = fa match {
      case Resolve(_, p) => fun(p)
      case _ => failTest("Should only be making a request for a single package")
    }
  }

  property("The ResolveOne endpoint returns the correct package name for a Google Play Store app") =
    forAll { (pkg: Package, item: Item) =>

      implicit val interpreter: Ops ~> Id = resolveOneInterpreter { p =>
        if (p == pkg) Some(item)
        else None
      }

      val route = NineCardsGooglePlayApi.googlePlayApiRoute[Id]

      resolveOne(pkg) ~> route ~> check {
        val response = responseAs[String]
        (status ?= OK) && (decode[Item](response) ?= Xor.Right(item))
      }
    }

  property("fails with an Internal Server Error when the package is not known") =
    forAll {(unknownPackage: Package, wrongItem: Item) =>

      implicit val interpreter: Ops ~> Id = resolveOneInterpreter { p =>
        if (p == unknownPackage) None
        else Some(wrongItem)
      }

      val route = NineCardsGooglePlayApi.googlePlayApiRoute[Id]

      resolveOne(unknownPackage) ~> route ~> check {
        status ?= InternalServerError
      }
    }

  def resolveMany(packageList: PackageList) =
    Post("/googleplay/packages/detailed", packageList) ~> addHeaders(requestHeaders)

  def resolveManyInterpreter(fun: PackageList => PackageDetails): Ops ~> Id = new (Ops ~> Id){
    def apply[A](fa: Ops[A]) = fa match {
      case ResolveMany(_, packageList) => fun(packageList)
      case _ => failTest("Should only be making a bulk request")
    }
  }

  property("gives the package details for the known packages and highlights the errors") =
    forAll(genPick[Package, Item]) { (data: (Map[Package, Item], List[Package], List[Package])) =>

      val (database, succs, errs) = data

      //order doesn't matter
      val errors = errs.map(_.value).toSet
      val items = succs.map(i => database(i)).toSet

      implicit val interpreter: Ops ~> Id = resolveManyInterpreter { case PackageList(packages) =>
        val fetched: List[Xor[String, Item]] = packages.map{ i =>
          database.get(Package(i)).toRightXor(i)
        }

        fetched.foldLeft(PackageDetails(Nil, Nil)) { case (PackageDetails(errors, items), next) =>
          next.fold(s => PackageDetails(s :: errors, items), i => PackageDetails(errors, i :: items))
        }
      }

      val route = NineCardsGooglePlayApi.googlePlayApiRoute[Id]

      val allPackages = (succs ++ errs).map(_.value)

      resolveMany( PackageList(allPackages)) ~> route ~> check {
        val response = responseAs[String]
        val decoded = decode[PackageDetails](response)
          .getOrElse(throw new RuntimeException(s"Unable to parse response [$response]"))
        (status ?= OK) &&
        (decoded.errors.toSet ?= errors) &&
        (decoded.items.toSet ?= items)
      }
    }

  def getCard(pkg: Package) =
    Get(s"/googleplay/cards/${pkg.value}") ~> addHeaders(requestHeaders)

  def getCardInterpreter( fun: Package => Xor[AppMissed,AppCard]) = new (Ops ~> Id){
    def apply[A](fa: Ops[A]) = fa match {
      case GetCard(_, p) => fun(p)
      case _ => failTest("Should only be making a request for an AppCard")
    }
  }

  property(""" GET '/googleplay/cards/{pkg}' returns a valid card for an app"""") =
    forAll { (pkg: Package, card: AppCard) =>

      implicit val interpreter = getCardInterpreter { p =>
        if (p == pkg) Xor.Right(card)
        else Xor.Left( AppMissed( packageName = p.value, cause = ""))
      }

      val route = NineCardsGooglePlayApi.googlePlayApiRoute[Id]

      getCard(pkg) ~> route ~> check {
        val response = responseAs[String]
          (status ?= OK) && (decode[AppCard](response) ?= Xor.Right(card))
      }
    }

  property(""" GET '/googleplay/cards/{pkg}' fails with a NotFound Error when the package is not known""") =
    forAll {(unknownPackage: Package, wrongCard: AppCard) =>

      val appMissed = AppMissed(packageName = unknownPackage.value, cause = "")

      implicit val interpreter = getCardInterpreter{ p =>
        if (p == unknownPackage) Xor.Left(appMissed)
        else Xor.Right(wrongCard)
      }

      val route = NineCardsGooglePlayApi.googlePlayApiRoute[Id]

      getCard(unknownPackage) ~> route ~> check {
        val response = responseAs[String]
          (status ?= NotFound) && (decode[AppMissed](response) ?= Xor.Right( appMissed))
      }
    }
}
