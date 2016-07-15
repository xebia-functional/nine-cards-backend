package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import scalaz.concurrent.Task

class TaskInterpreter(appService: AppService) extends (GooglePlay.Ops ~> Task) {

  def apply[A](fa: GooglePlay.Ops[A]): Task[A] = fa match {

    case GooglePlay.Resolve(auth, pkg) =>
      appService( AppRequest(pkg, auth) ).map(_.toOption)

    case GooglePlay.ResolveMany(auth, PackageList(packageNames)) =>

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
