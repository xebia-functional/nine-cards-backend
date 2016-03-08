package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain.{ GoogleAuthParams, Localization }
import scalaz.concurrent.Task
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import cats.~>
import cats.data.Xor
import cats.data.XorT
import cats.std.list._
import cats.std.option._
import cats.syntax.traverse._
import cats.Foldable

object TaskInterpreter {

  def interpreter(
    apiRequest: (Package, GoogleAuthParams) => Task[Xor[String, Item]],
    webRequest: (Package, Option[Localization]) => Task[Xor[String, Item]]
  ) = new (GooglePlayOps ~> Task) {
    def apply[A](fa: GooglePlayOps[A]) = fa match {
      case RequestPackage((token, androidId, localizationOption), pkg) =>
        // there is a smell here in that api request can "fail",
        // returning left xor, but also fail by the task being a failed state
        // it's possible with this configuration for the web request to be called twice
        // i should write a test to guard against that.
        XorT(apiRequest(pkg, (token, androidId, localizationOption)).or(webRequest(pkg, localizationOption)))
          .orElse(XorT(webRequest(pkg, localizationOption)))
          .to[Option]

      case BulkRequestPackage((token, androidId, localizationOption), PackageListRequest(packageNames)) =>
        val packages: List[Package] = packageNames.map(Package.apply)

        val fetched: Task[List[Xor[String, Item]]] = packages.traverse{ p =>
          apiRequest(p, (token, androidId, localizationOption))
        }

        fetched.map { (xors: List[Xor[String, Item]]) =>
          Foldable[List].foldMap(xors) { xor =>
            xor.fold(e => PackageDetails(List(e), Nil), i => PackageDetails(Nil, List(i)))
          }
        }
    }
  }
}
