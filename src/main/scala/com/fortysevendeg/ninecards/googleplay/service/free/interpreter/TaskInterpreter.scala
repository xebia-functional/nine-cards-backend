package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import scalaz.concurrent.Task
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import cats.~>
import cats.data.Xor
import cats.syntax.option._
import cats.std.list._
import cats.syntax.traverse._
import cats.Foldable

object TaskInterpreter {

  implicit val interpreter = new (GooglePlayOps ~> Task) {
    def apply[A](fa: GooglePlayOps[A]) = fa match {
      case RequestPackage((token, androidId, localizationOption), pkg) =>
        Http4sGooglePlayApiClient.request(pkg, Http4sGooglePlayApiClient.headers(token, androidId, localizationOption)).map(_.fold(_ => None: Option[Item], Some(_)))

      case BulkRequestPackage((token, androidId, localizationOption), PackageListRequest(packageNames)) =>
        val packages: List[Package] = packageNames.map(Package.apply)

        val hs = Http4sGooglePlayApiClient.headers(token, androidId, localizationOption)

        val fetched: Task[List[Xor[String, Item]]] = packages.traverse{ p =>
          Http4sGooglePlayApiClient.request(p, hs)
        }

        fetched.map { (xors: List[Xor[String, Item]]) =>
          Foldable[List].foldMap(xors) { xor =>
            xor.fold(e => PackageDetails(List(e), Nil), i => PackageDetails(Nil, List(i)))
          }
        }
    }
  }
}


