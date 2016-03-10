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
  ) = {

    def composedRequests(pkg: Package, auth: GoogleAuthParams): XorT[Task, String, Item] = {
      val (t, id, lo) = auth

      // Currently this results in the webrequest being called twice
      // if the API request fails and the Web Request returns an Xor.left
      XorT(apiRequest(pkg, (t, id, lo)).or(webRequest(pkg, lo)))
        .orElse(XorT(webRequest(pkg, lo)))
    }

    new (GooglePlayOps ~> Task) {
      def apply[A](fa: GooglePlayOps[A]) = fa match {
        case RequestPackage(auth, pkg) =>
          composedRequests(pkg, auth).to[Option]

        case BulkRequestPackage(auth, PackageListRequest(packageNames)) =>
          val packages: List[Package] = packageNames.map(Package.apply)

          val fetched: Task[List[Xor[String, Item]]] = packages.traverse{ p =>
            composedRequests(p, auth).value
          }

          fetched.map { (xors: List[Xor[String, Item]]) =>
            Foldable[List].foldMap(xors) { xor =>
              xor.fold(e => PackageDetails(List(e), Nil), i => PackageDetails(Nil, List(i)))
            }
          }
      }
    }
  }
}
