package com.fortysevendeg.ninecards.api // todo this package name is wrong

import spray.routing._
import akka.actor.Actor
import shapeless._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import com.fortysevendeg.ninecards.googleplay.ninecardsspray._
import Domain._
import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Unmarshaller
import spray.http.HttpEntity
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayService._
import com.fortysevendeg.ninecards.googleplay.service.Http4sGooglePlayService
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayCrawler
import scalaz.concurrent.Task

import cats._
import cats.Monad
import cats.Traverse
import cats.std.option._
import cats.std.list._
import cats.syntax.traverse._
import cats.syntax.option._
import cats.data.Xor

class NineCardsGooglePlayActor extends Actor with NineCardsGooglePlayApi {

  def actorRefFactory = context

  def receive = runRoute(googlePlayApiRoute(Http4sGooglePlayService.packageRequest _))
}

object Domain {
  case class Package(value: String) extends AnyVal

  case class PackageListRequest(items: List[String]) extends AnyVal
  case class PackageDetails(errors: List[String], items: List[Item])

  case class Item(docV2: DocV2)
  case class DocV2(title: String, creator: String, docid: String, details: Details, aggregateRating: AggregateRating, image: List[Image], offer: List[Offer])
  case class Details(appDetails: AppDetails)
  case class AppDetails(appCategory: List[String], numDownloads: String, permission: List[String])
  case class AggregateRating(ratingsCount: Long, oneStarRatings: Long, twoStarRatings: Long, threeStarRatings: Long, fourStarRatings: Long, fiveStarRatings: Long, starRating: Double) // commentcount?
  case class Image(imageType: Long, imageUrl: String) // todo check which fields are necessary here
  case class Offer(offerType: Long) // todo check which fields are necessary here
}

trait NineCardsGooglePlayApi extends HttpService {

  //type Headers = Token :: AndroidId :: Option[Localisation] :: HNil


  // todo this has been lifted straight from the backend project
  // put this (and the backend code) in its own project
  import spray.httpx.marshalling.ToResponseMarshaller

  implicit def taskInstance = new Monad[Task] {
    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] =
      fa.flatMap(f)

    override def pure[A](a: A): Task[A] = Task.now(a)
  }

  implicit def tasksMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Task[A]] =
    ToResponseMarshaller[Task[A]] {
      (task, ctx) =>
        task.runAsync {
          _.fold(
            left => ctx.handleError(left),
            right => m(right, ctx))
        }
    }

  /////

  ///////////// Feels like this will disappear or at least live somewhere else (and become a 404)

  import spray.httpx.marshalling.ToResponseMarshaller
  import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}

  implicit def optionalItemMarshaller(implicit im: ToResponseMarshaller[Item]): ToResponseMarshaller[Option[Item]] = {
    ToResponseMarshaller[Option[Item]] { (o, ctx) =>
      o.fold(ctx.marshalTo(HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("Cannot find item!"))))(im(_, ctx))
    }
  }

  //////////

  val requestHeaders = for {
    token        <- headerValueByName("X-Google-Play-Token")
    androidId    <- headerValueByName("X-Android-ID")
    localisation <- optionalHeaderValueByName("X-Android-Market-Localization")
  } yield Token(token) :: AndroidId(androidId) :: localisation.map(Localization.apply) :: HNil

  // todo I should be able to make this generic and move it into the package object
  implicit val packageListUnmarshaller: Unmarshaller[PackageListRequest] = new Unmarshaller[PackageListRequest] {
    def apply(entity: HttpEntity) = {
      decode[PackageListRequest](entity.asString).fold(e => Left(MalformedContent("Unable to parse entity into JSON list", e)), s => Right(s))
    }
  }

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

              val fetched: Task[List[Xor[String, Item]]] = packageNames.map { p =>
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
