package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

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

      for /*Task*/ {
        xors <- packageNames.traverse{ pkg => 
          appService( AppRequest(Package(pkg), auth))
        }
        (errors, apps) = splitXors[String, Item](xors)
      } yield PackageDetails(errors, apps)

  }

}

object TaskInterpreter {

  def apply( one: AppService, two: AppService ): TaskInterpreter =
    new TaskInterpreter( new XorTaskOrComposer[AppRequest,String,Item](one, two))

}
