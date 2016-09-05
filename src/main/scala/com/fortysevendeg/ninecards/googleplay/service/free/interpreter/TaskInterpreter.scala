package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import cats.~>
import com.fortysevendeg.extracats._
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay
import scalaz.concurrent.Task

class TaskInterpreter(
  itemService: AppRequest => Task[Xor[String,Item]],
  appCardService: AppRequest => Task[Xor[InfoError,AppCard]],
  recommendByCategory: GooglePlay.RecommendationsByCategory => Task[Xor[InfoError, AppRecommendationList]],
  recommendByAppList: GooglePlay.RecommendationsByAppList => Task[AppRecommendationList]
) extends (GooglePlay.Ops ~> Task) {

  def apply[A](fa: GooglePlay.Ops[A]): Task[A] = fa match {

    case GooglePlay.Resolve(auth, pkg) =>
      itemService( AppRequest(pkg, auth) ).map(_.toOption)

    case GooglePlay.ResolveMany(auth, PackageList(packageNames)) =>
      for /*Task*/ {
        xors <- packageNames.traverse{ pkg => 
          itemService( AppRequest(Package(pkg), auth))
        }
        (errors, apps) = splitXors[String, Item](xors)
      } yield PackageDetails(errors, apps)

    case GooglePlay.GetCard(auth, pkg) =>
      appCardService( AppRequest(pkg, auth) )

    case GooglePlay.GetCardList( auth, PackageList(packageNames)) =>
      for /*Task*/ {
        xors <- packageNames.traverse{ pkg =>
          appCardService( AppRequest(Package(pkg), auth))
        }
        (errors, apps) = splitXors[InfoError, AppCard](xors)
      } yield AppCardList(errors.map(_.message), apps)

    case message @ GooglePlay.RecommendationsByCategory(_,_,_) =>
      recommendByCategory( message)

    case message @ GooglePlay.RecommendationsByAppList(_,_) =>
      recommendByAppList( message )
  }

}
