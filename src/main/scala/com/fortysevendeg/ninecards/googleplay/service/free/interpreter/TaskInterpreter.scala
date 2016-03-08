package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain.GoogleAuthParams
import scalaz.concurrent.Task
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import cats.~>
import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import cats.Foldable

object TaskInterpreter {

  def interpreter(apiRequest: (Package, GoogleAuthParams) => Task[Xor[String, Item]]) = new (GooglePlayOps ~> Task) {
    def apply[A](fa: GooglePlayOps[A]) = fa match {
      case RequestPackage((token, androidId, localizationOption), pkg) =>
        apiRequest(pkg, (token, androidId, localizationOption)).map(_.toOption)

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
