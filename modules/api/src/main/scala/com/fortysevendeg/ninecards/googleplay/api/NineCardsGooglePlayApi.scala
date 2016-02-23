package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayService._
import com.fortysevendeg.ninecards.googleplay.service.Http4sGooglePlayService
import com.fortysevendeg.extracats._
import cats._
import cats.Traverse
import cats.std.option._
import cats.std.list._
import cats.syntax.traverse._
import cats.syntax.option._
import cats.data.Xor
import spray.routing._
import akka.actor.Actor
import shapeless._
import scalaz.concurrent.Task
import io.circe.generic.auto._

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute(Http4sGooglePlayService.packageRequest _))
}

trait NineCardsGooglePlayApi extends HttpService {

  val requestHeaders = for {
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

  def googlePlayApiRoute(getPackage: (GoogleAuthParams) => Package => Task[Option[Item]]) =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localisationOption) =>
        get {
          path("package" / Segment) { packageName => // TODO make this a package type
            val packageDetails = getPackage((token, androidId, localisationOption))(Package(packageName))
            complete(packageDetails)
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageListRequest]) { case PackageListRequest(packageNames) =>

              val packageFetcher = getPackage((token, androidId, localisationOption))

              val fetched: Task[List[Xor[String, Item]]] = packageNames.map{ p =>
                packageFetcher(Package(p)).map(_.toRightXor[String](p))
              }.sequenceU

              val details: Task[PackageDetails] = fetched.map { (xors: List[Xor[String, Item]]) =>
                xors.foldLeft(PackageDetails(Nil, Nil)) { case (PackageDetails(errors, items), xor) =>
                  xor.fold(s => PackageDetails(s :: errors, items), i => PackageDetails(errors, i :: items))
                }
              }

              complete(details)
            }
          }
        }
      }
    }
}
