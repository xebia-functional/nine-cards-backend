package cards.nine.googleplay.processes

import cats.data.Xor
import cats.free.Free
import cats.instances.list._
import cats.syntax.monadCombine._
import cats.syntax.traverse._
import cats.syntax.xor._
import cards.nine.domain.application.{ FullCard, FullCardList, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle.{ Failure ⇒ ApiFailure }
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
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
        _ ← cacheService.clearInvalid(card.packageName)
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

    def storeAsError(pack: Package): Free[F, Unit] =
      for {
        _ ← cacheService.unmarkPending(pack)
        _ ← cacheService.markError(pack)
      } yield Unit

  }

  def getBasicCards(
    packages: List[Package],
    auth: MarketCredentials
  ): Free[F, ResolveMany.Response] =
    for {
      cacheResult ← checkValidPackagesInCache(packages)
      (uncached, cached) = cacheResult
      response ← getPackagesInfoInGooglePlay(uncached, auth)
    } yield ResolveMany.Response(response.notFound, response.pending, cached ++ response.apps)

  def getCard(pack: Package, auth: MarketCredentials): Free[F, getcard.Response] =
    resolveNewPackage(pack, auth)

  def getCards(
    packages: List[Package],
    auth: MarketCredentials
  ): Free[F, List[getcard.Response]] =
    packages.traverse[Free[F, ?], getcard.Response](pack ⇒ resolveNewPackage(pack, auth))

  def recommendationsByCategory(
    request: RecommendByCategoryRequest,
    auth: MarketCredentials
  ): Free[F, InfoError Xor FullCardList] =
    googleApi.recommendationsByCategory(request, auth) flatMap {
      case ll @ Xor.Left(_) ⇒ Free.pure(ll)
      case Xor.Right(recommendations) ⇒
        val packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
        resolveNewPackageList(packages, auth).map(_.right[InfoError])
    }

  def recommendationsByApps(
    request: RecommendByAppsRequest,
    auth: MarketCredentials
  ): Free[F, FullCardList] =
    for /*Free[F,?]*/ {
      recommendations ← googleApi.recommendationsByApps(request, auth)
      packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
      resolvedPackages ← resolveNewPackageList(packages, auth)
    } yield resolvedPackages

  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): Free[F, FullCardList] =
    googleApi.searchApps(request, auth) flatMap {
      case Xor.Left(_) ⇒ Free.pure(FullCardList(Nil, Nil))
      case Xor.Right(packs) ⇒ resolveNewPackageList(packs, auth)
    }

  private[this] def checkValidPackagesInCache(packages: List[Package]) =
    packages.traverse[Free[F, ?], Package Xor FullCard] { p ⇒
      cacheService.getValid(p) map (card ⇒ Xor.fromOption(card, p))
    } map (_.separate)

  private[this] def getPackagesInfoInGooglePlay(packages: List[Package], auth: MarketCredentials) =
    googleApi.getBulkDetails(packages, auth) map {
      case Xor.Left(_) ⇒
        ResolveMany.Response(Nil, packages, Nil)
      case Xor.Right(apps) ⇒
        val notFound = packages.diff(apps.map(_.packageName))
        ResolveMany.Response(notFound, Nil, apps)
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
        for (status ← resolvePendingPackage(pack, date)) yield (pack, status)
      }
    } yield splitStatus(status)

  }

  def resolveNewPackageList(packs: List[Package], auth: MarketCredentials): Free[F, FullCardList] =
    for {
      xors ← packs.traverse[Free[F, ?], getcard.Response](p ⇒ resolveNewPackage(p, auth))
      (fails, apps) = xors.separate
    } yield FullCardList(fails.map(e ⇒ e.packageName), apps)

  def resolveNewPackage(pack: Package, auth: MarketCredentials): Free[F, getcard.Response] = {

    import getcard._

    // Third step: handle error and ask for package in Google Play
    def handleFailedResponse(failed: ApiFailure): Free[F, FailedResponse] = {
      // Does package exists in Google Play?
      webScrapper.existsApp(pack) flatMap {
        case true ⇒
          InCache.storeAsPending(pack).map(_ ⇒ PendingResolution(pack))
        case false ⇒
          cacheService.markError(pack).map(_ ⇒ UnknownPackage(pack))
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
        for (_ ← InCache.storeAsError(pack)) yield Unknown
      case Xor.Left(WebPageServerError) ⇒
        Free.pure(Pending)
    }
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

