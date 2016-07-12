package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.{ BulkRequestPackage, RequestPackage }
import org.scalacheck._
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.scalacheck.Gen._
import org.scalacheck.Shapeless._
import scalaz.concurrent.Task
import cats.data.Xor
import cats.syntax.xor._

object TaskInterpreterProperties extends Properties("Task interpreter") {

  implicit val arbItem: Arbitrary[Item] = Arbitrary(for {
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

  object exceptionalRequest extends AppService {
    def apply(req: AppRequest) = Task.fail(new RuntimeException("API request failed"))
  }

  object failingRequest extends AppService {
    def apply(req: AppRequest) = Task.now( req.packageName.value.left )
  }

  property("Requesting a single package should pass the correct parameters to the client request") = forAll { (pkg: Package, i: Item, t: Token, id: AndroidId, lo: Option[Localization]) =>
    val request = RequestPackage(GoogleAuthParams(id, t, lo), pkg)

    object f extends AppService {
      def apply(req: AppRequest) = {
        val res = req match {
          case AppRequest(`pkg`, GoogleAuthParams(`id`, `t`, `lo`) ) => i.right
          case _ => req.packageName.value.left
        }
        Task.now(res)
      }
    }

    val interpreter = TaskInterpreter(f, exceptionalRequest)

    val response = interpreter(request)

    response.run ?= Some(i)
  }

  property("Requesting multiple packages should call the API for the given packages and no others") = forAll { (ps: List[Package], i: Item, t: Token, id: AndroidId, lo: Option[Localization]) =>

    val packageNames = ps.map(_.value)

    val request = BulkRequestPackage(GoogleAuthParams(id, t, lo), PackageList(packageNames))

    val f: AppService = { case AppRequest(pkgParam, GoogleAuthParams(idParam, tParam, loParam)) =>
      Task.now {
        (tParam, idParam, loParam) match {
          case (`t`, `id`, `lo`) if(ps.contains(pkgParam)) => i.right
          case _ => pkgParam.value.left
        }
      }
    }

    val interpreter = TaskInterpreter(f, exceptionalRequest)

    val response = interpreter(request)

    val packageDetails = response.run

    (s"Should have not errored for any request: ${packageDetails.errors}" |: (packageDetails.errors ?= Nil)) &&
    (s"Should have successfully returned for each given package: ${packageDetails.items.length}" |: (packageDetails.items.length ?= ps.length))
  }

  property("An unsuccessful API call for a single package falls back to the web request") = forAll { (pkg: Package, i: Item, auth: GoogleAuthParams) =>

    val request = RequestPackage(auth, pkg)

    val webRequest: AppService = ( r => Task.now( if (r.packageName == pkg) i.right else r.packageName.value.left ) )

    val interpreter = TaskInterpreter(failingRequest, webRequest)

    val response = interpreter(request)

    response.run ?= Some(i)
  }

  property("Unsuccessful API calls when working with bulk packages will fall back to the web request") = forAllNoShrink { (rawApiPackages: List[Package], apiItem: Item, rawWebPackages: List[Package], webItem: Item, auth: GoogleAuthParams) =>
    (apiItem != webItem) ==> {

      // make sure there are no clashes between the two sets of names
      val apiPackages = rawApiPackages.map{case Package(name) => Package(s"api$name")}
      val webPackages = rawWebPackages.map{case Package(name) => Package(s"web$name")}

      def makeRequestFunc(ps: List[Package], toReturn: Item): AppRequest => Task[Xor[String, Item]] = { req =>
        Task.now {
          if(ps.contains(req.packageName)) toReturn.right
          else req.packageName.value.left
        }
      }

      val apiRequest: AppService = makeRequestFunc(apiPackages, apiItem)
      val webRequest: AppService = makeRequestFunc(webPackages, webItem)

      val packageNames = (apiPackages ::: webPackages).map(_.value)
      val request = BulkRequestPackage(auth, PackageList(packageNames))

      val interpreter = TaskInterpreter(apiRequest, webRequest)

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

    val interpreter = TaskInterpreter(failingRequest, failingRequest)

    val response = interpreter(request)

    response.run ?= None
  }

  property("Unsuccessful API and web requests when working with bulk packages results in collected errors in the response") = forAll { (packages: List[Package], auth: GoogleAuthParams) =>

    val packageNames = packages.map(_.value)

    val request = BulkRequestPackage(auth, PackageList(packageNames))

    val interpreter = TaskInterpreter(failingRequest, failingRequest)

    val response = interpreter(request)

    val PackageDetails(errors, items) = response.run

    (items ?= Nil) &&
    (errors ?= packageNames)
  }

  property("A failed task in the API call results in the web request being made") = forAll { (pkg: Package, webResponse: Xor[String, Item], auth: GoogleAuthParams) =>

    val request = RequestPackage(auth, pkg)

    val successfulWebRequest: AppService = { q =>
      if(q.packageName == pkg) Task.now(webResponse)
      else Task.fail(new RuntimeException("Exception thrown by task when it should not be"))
    }

    val interpreter = TaskInterpreter(exceptionalRequest, successfulWebRequest)

    val response = interpreter(request)

    response.run ?= webResponse.toOption
  }
}
