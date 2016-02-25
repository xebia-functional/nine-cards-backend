package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.service.Http4sGooglePlayService
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
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

trait NewApi extends HttpService {

  import spray.httpx.marshalling.ToResponseMarshaller
  import cats.free.Free

  val requestHeaders = for { // todo put this in a package object somewhere
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

  def newRoute[M[_]: Monad](
    implicit
      googlePlayService: GooglePlayService[GooglePlayOps],
      interpreter: GooglePlayOps ~> M, // todo can this be made GPO ~> TRM
      itemMarshaller: ToResponseMarshaller[M[Option[Item]]], // todo need to make the option[item] generic
      bulkMarshaller: ToResponseMarshaller[M[PackageDetails]]
  ): Route =
    pathPrefix("googleplay") {
      requestHeaders { (token, androidId, localizationOption) =>
        get {
          path("package" / Segment) { packageName =>
            complete {
              googlePlayService.requestPackage((token, androidId, localizationOption), Package(packageName)).foldMap(interpreter)
            }
          }
        } ~
        post {
          path("packages" / "detailed") {
            entity(as[PackageListRequest]) { req =>
              complete {
                googlePlayService.bulkRequestPackage((token, androidId, localizationOption), req).foldMap(interpreter)
              }
            }
          }
        }
      }
    }
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

              val fetched: Task[List[Xor[String, Item]]] = packageNames.traverse{ p =>
                packageFetcher(Package(p)).map(_.toRightXor[String](p))
              }

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
