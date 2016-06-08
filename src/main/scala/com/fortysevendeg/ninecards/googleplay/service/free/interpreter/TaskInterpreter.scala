package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.Foldable
import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import scalaz.concurrent.Task

class TaskInterpreter(queryService: QueryService) extends (GooglePlayOps ~> Task) {

  def apply[A](fa: GooglePlayOps[A]): Task[A] = fa match {

    case RequestPackage(auth, pkg) => queryService(pkg, auth).map(_.toOption)

    case BulkRequestPackage(auth, PackageListRequest(packageNames)) =>

      def fetchDetails(packageName: String): Task[PackageDetails] = {
        def toDetails(xor: Xor[String, Item]): PackageDetails = xor.fold(
          e => PackageDetails(List(e), Nil),
          i => PackageDetails(Nil, List(i))
        )
        queryService(Package(packageName), auth).map(toDetails)
      }
      packageNames.traverse(fetchDetails).map(Foldable[List].fold(_))
  }

}

object TaskInterpreter {

  def apply( one: QueryService, two: QueryService ): TaskInterpreter =
    new TaskInterpreter( new XorTaskOrComposer1[QueryRequest,String,Item](one, two))

}
