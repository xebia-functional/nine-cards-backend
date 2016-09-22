package com.fortysevendeg.ninecards.googleplay.processes

import cats.data.Xor
import cats.free.Free
import cats.std.list._
import cats.syntax.traverse._
import com.fortysevendeg.ninecards.googleplay.domain.{Package, GoogleAuthParams, FullCard}
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle.{Failure => ApiFailure}
import com.fortysevendeg.ninecards.googleplay.domain.webscrapper._
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

    def storeAsError( pack: Package, date: DateTime): Free[F, Unit] =
      for {
        _ <- cacheService.unmarkPending(pack)
        _ <- cacheService.markError(pack, date)
      } yield Unit

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

  def resolvePendingPackage(pack: Package, date: DateTime): Free[F, ResolvePending.PackageStatus] = {
    import ResolvePending._

    webScrapper.getDetails(pack) flatMap {
      case Xor.Right(card) =>
        for (_ <- InCache.storeAsResolved(card)) yield Resolved
      case Xor.Left( PageParseFailed(_) ) =>
        for (_ <- InCache.storeAsError(pack, date) ) yield Unknown
      case Xor.Left( PackageNotFound(_) ) =>
        for (_ <- InCache.storeAsError(pack, date) ) yield Unknown
      case Xor.Left( WebPageServerError ) =>
        Free.pure(Pending)
    }
  }

  def searchAndResolvePending( numApps: Int, date: DateTime) : Free[F, Unit] = {
    import ResolvePending._

    def splitStatus( stati: List[(Package, PackageStatus)]): Response = {
      val solved  = stati collect { case (pack,Resolved) => pack }
      val unknown = stati collect { case (pack,Unknown) => pack }
      val pending = stati collect { case (pack,Pending) => pack }
      Response(solved, unknown, pending)
    }

    type FF[A] = Free[F,A]

    for /*Free[F] */ {
      list <- cacheService.listPending(numApps)
      stati <- list.traverse[FF,(Package, PackageStatus) ]{ pack =>
        for (status <- resolvePendingPackage(pack, date)) yield (pack, status)
      }
    } yield splitStatus(stati)

  }

}

object CardsProcess {

  implicit def processes[F[_]](
    implicit apiS: apigoogle.Service[F],
    cacheS: cache.Service[F],
    webS: webscrapper.Service[F]
  ): CardsProcess[F] = new CardsProcess(apiS, cacheS, webS)
}


