package cards.nine.googleplay.processes

import cats.data.Xor
import cats.free.Free
import cats.instances.list._
import cats.syntax.traverse._
import cards.nine.commons.FreeUtils._
import cards.nine.googleplay.domain.{ FullCard, FullCardList, GoogleAuthParams, InfoError, Package, RecommendByAppsRequest, RecommendByCategoryRequest }
import cards.nine.googleplay.domain.apigoogle.{ Failure ⇒ ApiFailure }
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cards.nine.googleplay.service.free.interpreter.googleapi.Converters
import org.joda.time.DateTime

class CardsProcesses[F[_]](
  googleApi: GoogleApi.Services[F],
  cacheService: Cache.Service[F],
  webScrapper: WebScraper.Service[F]
) {

  private[this] object InCache {
    def storeAsResolved(card: FullCard): Free[F, Unit] =
      for {
        _ ← cacheService.putResolved(card)
        _ ← cacheService.clearInvalid(Package(card.packageName))
      } yield Unit

    def storeAsPending(pack: Package): Free[F, Unit] =
      cacheService.isPending(pack) flatMap {
        case true ⇒ Free.pure(Unit)
        case false ⇒
          for /*Free[F]*/ {
            _ ← cacheService.clearInvalid(pack)
            _ ← cacheService.markPending(pack)
          } yield Unit
      }

    def storeAsError(pack: Package, date: DateTime): Free[F, Unit] =
      for {
        _ ← cacheService.unmarkPending(pack)
        _ ← cacheService.markError(pack, date)
      } yield Unit

  }

  private[this] object PackagesResolution {

    def resolveNewPackage(pack: Package, auth: GoogleAuthParams, date: DateTime): Free[F, getcard.Response] = {

      import getcard._

      // Third step: handle error and ask for package in Google Play
      def handleFailedResponse(failed: ApiFailure): Free[F, FailedResponse] = {
        // Does package exists in Google Play?
        webScrapper.existsApp(pack) flatMap {
          case true ⇒
            InCache.storeAsPending(pack).map(_ ⇒ PendingResolution(pack))
          case false ⇒
            cacheService.markError(pack, date).map(_ ⇒ UnknownPackage(pack))
        }
      }

      // "Resolved or permanent Item in Redis Cache?"
      cacheService.getValid(pack) flatMap {
        case Some(card) ⇒
          // Yes -> Return the stored content
          Free.pure(Xor.Right(card))
        case None ⇒
          // No -> Google Play API Returns valid response?
          googleApi.getDetails(pack, auth) flatMap {
            case r @ Xor.Right(card) ⇒
              // Yes -> Create key/value in Redis as Resolved, Return package info
              InCache.storeAsResolved(card).map(_ ⇒ r)
            case Xor.Left(apiFailure) ⇒
              handleFailedResponse(apiFailure).map(Xor.left)
          }
      }
    }

    def resolvePendingPackage(pack: Package, date: DateTime): Free[F, ResolvePending.PackageStatus] = {
      import ResolvePending._

      webScrapper.getDetails(pack) flatMap {
        case Xor.Right(card) ⇒
          for (_ ← InCache.storeAsResolved(card)) yield Resolved
        case Xor.Left(PageParseFailed(_)) ⇒
          for (_ ← InCache.storeAsPending(pack)) yield Pending
        case Xor.Left(PackageNotFound(_)) ⇒
          for (_ ← InCache.storeAsError(pack, date)) yield Unknown
        case Xor.Left(WebPageServerError) ⇒
          Free.pure(Pending)
      }
    }
  }

  def getCard(pack: Package, auth: GoogleAuthParams, date: DateTime): Free[F, getcard.Response] =
    PackagesResolution.resolveNewPackage(pack, auth, date)

  def getCards(
    packages: List[Package],
    auth: GoogleAuthParams,
    date: DateTime
  ): Free[F, List[getcard.Response]] =
    packages.traverse[Free[F, ?], getcard.Response](pack ⇒ PackagesResolution.resolveNewPackage(pack, auth, date))

  def recommendationsByCategory(
    request: RecommendByCategoryRequest,
    auth: GoogleAuthParams
  ): Free[F, InfoError Xor FullCardList] = {

    val recommendations = for {
      recommendations ← googleApi.recommendationsByCategory(request, auth).toXorT
      packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
      resolvedPackages ← packages.traverse[Free[F, ?], getcard.Response](
        p ⇒ PackagesResolution.resolveNewPackage(p, auth, DateTime.now)
      ).toXorTRight[InfoError]
    } yield resolvedPackages.map(xor ⇒ xor.bimap(e ⇒ InfoError(e.packageName.value), c ⇒ c))

    recommendations.map(Converters.toFullCardListXors).value
  }

  def recommendationsByApps(
    request: RecommendByAppsRequest,
    auth: GoogleAuthParams
  ): Free[F, FullCardList] = {

    val recommendations = for {
      recommendations ← googleApi.recommendationsByApps(request, auth)
      packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
      resolvedPackages ← packages.traverse[Free[F, ?], getcard.Response](
        p ⇒ PackagesResolution.resolveNewPackage(p, auth, DateTime.now)
      )
    } yield resolvedPackages.map(xor ⇒ xor.bimap(e ⇒ InfoError(e.packageName.value), c ⇒ c))

    recommendations.map(Converters.toFullCardListXors)
  }

  def searchAndResolvePending(numApps: Int, date: DateTime): Free[F, ResolvePending.Response] = {
    import ResolvePending._

    def splitStatus(status: List[(Package, PackageStatus)]): Response = {
      val solved = status collect { case (pack, Resolved) ⇒ pack }
      val unknown = status collect { case (pack, Unknown) ⇒ pack }
      val pending = status collect { case (pack, Pending) ⇒ pack }
      Response(solved, unknown, pending)
    }

    for {
      list ← cacheService.listPending(numApps)
      status ← list.traverse[Free[F, ?], (Package, PackageStatus)] { pack ⇒
        for (status ← PackagesResolution.resolvePendingPackage(pack, date)) yield (pack, status)
      }
    } yield splitStatus(status)

  }

}

object CardsProcesses {

  implicit def processes[F[_]](
    implicit
    apiS: GoogleApi.Services[F],
    cacheS: Cache.Service[F],
    webS: WebScraper.Service[F]
  ): CardsProcesses[F] = new CardsProcesses(apiS, cacheS, webS)
}

