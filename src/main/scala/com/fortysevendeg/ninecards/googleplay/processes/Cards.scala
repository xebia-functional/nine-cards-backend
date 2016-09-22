package com.fortysevendeg.ninecards.googleplay.processes

import cats.data.Xor
import cats.free.Free
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle.{Failure => ApiFailure}
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.{apigoogle, cache, webscrapper}
import org.joda.time.DateTime

class CardsProcess[F[_]](
  apiGoogle: apigoogle.Service[F],
  cacheService: cache.Service[F],
  webScrapper: webscrapper.Service[F]
) {

  private[this] object InCache {
    def storeAsResolved(card: FullCard): Free[F, Unit] =
      for {
        _ <- cacheService.putResolved(card)
        _ <- cacheService.clearInvalid(Package(card.packageName))
      } yield Unit

    def storeAsPending(pack: Package) : Free[F, Unit] =
      cacheService.isPending(pack) flatMap {
        case true => Free.pure(Unit)
        case false =>
          for /*Free[F]*/ {
            _ <- cacheService.clearInvalid(pack)
            _ <- cacheService.markPending(pack)
          } yield Unit
      }
  }

  def getCard( pack: Package, auth: GoogleAuthParams, date: DateTime): Free[F,getcard.Response] = {

    import getcard._

    // Third step: handle error and ask for package in Google Play
    def handleFailedResponse(failed: ApiFailure) : Free[F,FailedResponse] = {
      // Does package exists in Google Play?
      webScrapper.existsApp(pack) flatMap {
        case true =>
          InCache.storeAsPending(pack).map( _ => PendingResolution(pack) )
        case false =>
          cacheService.markError(pack, date).map( _ => UnknownPackage(pack))
      }
    }

    // "Resolved or permanent Item in Redis Cache?"
    cacheService.getValid(pack) flatMap {
      case Some(card) =>
        // Yes -> Return the stored content
        Free.pure(Xor.Right(card))
      case None =>
        // No -> Google Play API Returns valid response?
        apiGoogle.getDetails(pack, auth) flatMap {
          case r@Xor.Right(card) =>
            // Yes -> Create key/value in Redis as Resolved, Return package info
            InCache.storeAsResolved(card).map( _ => r)
          case Xor.Left(apiFailure) =>
            handleFailedResponse(apiFailure).map(Xor.left)
        }
    }
  }

}

object CardsProcess {

  implicit def processes[F[_]](
    implicit apiS: apigoogle.Service[F],
    cacheS: cache.Service[F],
    webS: webscrapper.Service[F]
  ): CardsProcess[F] = new CardsProcess(apiS, cacheS, webS)
}


package getcard {

  sealed trait FailedResponse
  case class WrongAuthParams( authParams: GoogleAuthParams) extends FailedResponse
  case class PendingResolution( packageName: Package) extends FailedResponse
  case class UnknownPackage( packageName: Package) extends FailedResponse

}

package object getcard {
  type Response = FailedResponse Xor FullCard
}