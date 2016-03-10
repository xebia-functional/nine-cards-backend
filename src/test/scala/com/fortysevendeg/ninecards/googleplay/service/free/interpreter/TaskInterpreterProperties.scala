package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.{ BulkRequestPackage, RequestPackage }
import org.scalacheck._
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.scalacheck.Gen._
import org.scalacheck.Shapeless._
import scalaz.concurrent.Task
import cats.data.Xor

object TaskInterpreterProperties extends Properties("Task interpreter") {

  implicit val arbItem: Arbitrary[Item] = Arbitrary(for {
    title <- identifier
    docId <- identifier
  } yield Item(
    DocV2(
      title   = title,
      creator = "",
      docid   = docId,
      details = Details(
        appDetails = AppDetails(
          appCategory  = List(),
          numDownloads = "",
          permission   = List()
        )
      ),
      aggregateRating = AggregateRating(
        ratingsCount     = 0,
        oneStarRatings   = 0,
        twoStarRatings   = 0,
        threeStarRatings = 0,
        fourStarRatings  = 0,
        fiveStarRatings  = 0,
        starRating       = 0.0
      ),
      image = List(),
      offer = List()
    )
  ))

  val exceptionalRequest: (Any, Any) => Task[Xor[String, Item]] = ((_, _) => Task.fail(new RuntimeException("API request failed")))

  val failingRequest: (Package, Any) => Task[Xor[String, Item]] = { case (Package(name), _) => Task.now(Xor.left(name))}

  property("Requesting a single package should pass the correct parameters to the client request") = forAll { (pkg: Package, i: Item, t: Token, id: AndroidId, lo: Option[Localization]) =>
    val request = RequestPackage((t, id, lo), pkg)

    val f: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (pkgParam, (tParam, idParam, loParam)) =>
      Task.now {
        (pkgParam, tParam, idParam, loParam) match {
          case (`pkg`, `t`, `id`, `lo`) => Xor.right(i)
          case _ => Xor.left(pkgParam.value)
        }
      }
    }

    val interpreter = TaskInterpreter.interpreter(f, exceptionalRequest)

    val response = interpreter(request)

    response.run ?= Some(i)
  }

  property("Requesting multiple packages should call the API for the given packages and no others") = forAll { (ps: List[Package], i: Item, t: Token, id: AndroidId, lo: Option[Localization]) =>

    val packageNames = ps.map(_.value)

    val request = BulkRequestPackage((t, id, lo), PackageListRequest(packageNames))

    val f: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (pkgParam, (tParam, idParam, loParam)) =>
      Task.now {
        (tParam, idParam, loParam) match {
          case (`t`, `id`, `lo`) if(ps.contains(pkgParam)) => Xor.right(i)
          case _ => Xor.left(pkgParam.value)
        }
      }
    }

    val interpreter = TaskInterpreter.interpreter(f, exceptionalRequest)

    val response = interpreter(request)

    val packageDetails = response.run

    (s"Should have not errored for any request: ${packageDetails.errors}" |: (packageDetails.errors ?= Nil)) &&
    (s"Should have successfully returned for each given package: ${packageDetails.items.length}" |: (packageDetails.items.length ?= ps.length))
  }

  property("An unsuccessful API call for a single package falls back to the web request") = forAll { (pkg: Package, i: Item, auth: GoogleAuthParams) =>

    val request = RequestPackage(auth, pkg)

    val apiRequest: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (Package(name), _) =>
      Task.now(Xor.left(name))
    }

    val webRequest: (Package, Option[Localization]) => Task[Xor[String, Item]] = { (p, _) =>
      Task.now {
        if (p == pkg) Xor.right(i)
        else Xor.left(p.value)
      }
    }

    val interpreter = TaskInterpreter.interpreter(apiRequest, webRequest)

    val response = interpreter(request)

    response.run ?= Some(i)
  }

  property("Unsuccessful API calls when working with bulk packages will fall back to the web request") = forAllNoShrink { (rawApiPackages: List[Package], apiItem: Item, rawWebPackages: List[Package], webItem: Item, auth: GoogleAuthParams) =>
    (apiItem != webItem) ==> {

      // make sure there are no clashes between the two sets of names
      val apiPackages = rawApiPackages.map{case Package(name) => Package(s"api$name")}
      val webPackages = rawWebPackages.map{case Package(name) => Package(s"web$name")}

      def makeRequestFunc(ps: List[Package], toReturn: Item): (Package, Any) => Task[Xor[String, Item]] = { (p, _) =>
        Task.now {
          if(ps.contains(p)) Xor.right(toReturn)
          else Xor.left(p.value)
        }
      }

      val apiRequest: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = makeRequestFunc(apiPackages, apiItem)
      val webRequest: (Package, Option[Localization]) => Task[Xor[String, Item]] = makeRequestFunc(webPackages, webItem)

      val packageNames = (apiPackages ::: webPackages).map(_.value)
      val request = BulkRequestPackage(auth, PackageListRequest(packageNames))

      val interpreter = TaskInterpreter.interpreter(apiRequest, webRequest)

      val response = interpreter(request)

      val PackageDetails(errors, items) = response.run

      val groupedItems = items.groupBy(identity).map{case (k, v) => (k, v.length)}
      val expectedGrouping = Map(apiItem -> apiPackages.length, webItem -> webPackages.length).filter{case (_, v) => v != 0}

      (errors ?= Nil) &&
      (groupedItems ?= expectedGrouping)
    }
  }

  property("Unsuccessful in both the API and web calls results in an unsuccessful response") = forAll { (pkg: Package, auth: GoogleAuthParams) =>

    val request = RequestPackage(auth, pkg)

    val interpreter = TaskInterpreter.interpreter(failingRequest, failingRequest)

    val response = interpreter(request)

    response.run ?= None
  }

  property("Unsuccessful API and web requests when working with bulk packages results in collected errors in the response") = forAll { (packages: List[Package], auth: GoogleAuthParams) =>

    val packageNames = packages.map(_.value)

    val request = BulkRequestPackage(auth, PackageListRequest(packageNames))

    val interpreter = TaskInterpreter.interpreter(failingRequest, failingRequest)

    val response = interpreter(request)

    val PackageDetails(errors, items) = response.run

    (items ?= Nil) &&
    (errors ?= packageNames)
  }

  property("A failed task in the API call results in the web request being made") = forAll { (pkg: Package, webResponse: Xor[String, Item], auth: GoogleAuthParams) =>

    val request = RequestPackage(auth, pkg)

    val successfulWebRequest: (Package, Option[Localization]) => Task[Xor[String, Item]] = { (p, _) =>
      if(p == pkg) Task.now(webResponse)
      else Task.fail(new RuntimeException("Exception thrown by task when it should not be"))
    }

    val interpreter = TaskInterpreter.interpreter(exceptionalRequest, successfulWebRequest)

    val response = interpreter(request)

    response.run ?= webResponse.toOption
  }
}
