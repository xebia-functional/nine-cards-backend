package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.{Foldable, Monoid}
import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import scalaz.concurrent.Task

class TaskInterpreter(appService: AppService) extends (GooglePlayOps ~> Task) {

  def apply[A](fa: GooglePlayOps[A]): Task[A] = fa match {

    case RequestPackage(auth, pkg) => appService( AppRequest(pkg, auth) ).map(_.toOption)

    case BulkRequestPackage(auth, PackageList(packageNames)) =>

      implicit object detailsMonoid extends Monoid[PackageDetails] {
        def empty: PackageDetails = PackageDetails(Nil, Nil)
        def combine(x: PackageDetails, y: PackageDetails): PackageDetails =
          PackageDetails(errors = x.errors ++ y.errors, items = x.items ++ y.items)
      }

      def fetchDetails(packageName: String): Task[PackageDetails] = {
        def toDetails(xor: Xor[String, Item]): PackageDetails = xor.fold(
          e => PackageDetails(List(e), Nil),
          i => PackageDetails(Nil, List(i))
        )
        appService( AppRequest(Package(packageName), auth) ).map(toDetails)
      }
      packageNames.traverse(fetchDetails).map(Foldable[List].fold(_))
  }

}

object TaskInterpreter {

  def apply( one: AppService, two: AppService ): TaskInterpreter =
    new TaskInterpreter( new XorTaskOrComposer[AppRequest,String,Item](one, two))

}
