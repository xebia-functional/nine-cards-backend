package cards.nine.googleplay.processes

import cats.free.Free
import cats.instances.either._
import cats.instances.list._
import cats.syntax.monadCombine._
import cats.syntax.traverse._
import cats.syntax.either._
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle.{ ResolvePackagesResult, Failure ⇒ ApiFailure, PackageNotFound ⇒ ApiNotFound }
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }

class CardsProcesses[F[_]](
  googleApi: GoogleApi.Services[F],
  cacheService: Cache.Service[F],
  webScrapper: WebScraper.Service[F]
) {

  def getBasicCards(
    packages: List[Package],
    auth: MarketCredentials
  ): Free[F, ResolveMany.Response[BasicCard]] =
    for {
      cached ← cacheService.getValidMany(packages)
      uncached = packages diff (cached map (_.packageName))
      response ← getPackagesInfoInGooglePlay(uncached, auth)
    } yield ResolveMany.Response(response.notFound, response.pending, cached.map(_.toBasic) ++ response.apps)

  def getCard(pack: Package, auth: MarketCredentials): Free[F, getcard.Response] =
    resolveNewPackage(pack, auth)

  def getCards(
    packages: List[Package],
    auth: MarketCredentials
  ): Free[F, ResolveMany.Response[FullCard]] =
    for {
      result ← resolvePackageList(packages, auth)
    } yield ResolveMany.Response(
      notFound = result.notFoundPackages,
      pending  = result.pendingPackages,
      apps     = result.resolvedPackages ++ result.cachedPackages
    )

  def storeCard(card: FullCard): Free[F, Unit] =
    cacheService.putPermanent(card)

  def recommendationsByCategory(
    request: RecommendByCategoryRequest,
    auth: MarketCredentials
  ): Free[F, InfoError Either CardList[FullCard]] =
    googleApi.recommendationsByCategory(request, auth) flatMap {
      case Left(error) ⇒ Free.pure(Either.left(error))
      case Right(recommendations) ⇒
        val packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
        for {
          result ← resolvePackageList(packages, auth)
        } yield Either.right(CardList(
          missing = result.notFoundPackages ++ result.pendingPackages,
          cards   = result.cachedPackages ++ result.resolvedPackages
        ))
    }

  def recommendationsByApps(
    request: RecommendByAppsRequest,
    auth: MarketCredentials
  ): Free[F, CardList[FullCard]] =
    for {
      recommendations ← googleApi.recommendationsByApps(request, auth)
      packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
      result ← resolvePackageList(packages, auth)
    } yield CardList(
      missing = result.notFoundPackages ++ result.pendingPackages,
      cards   = result.cachedPackages ++ result.resolvedPackages
    )

  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): Free[F, CardList[BasicCard]] =
    googleApi.searchApps(request, auth) flatMap {
      case Left(_) ⇒ Free.pure(CardList(Nil, Nil))
      case Right(packs) ⇒
        getBasicCards(packs, auth) map { r ⇒
          CardList(
            missing = r.notFound ++ r.pending,
            cards   = r.apps
          )
        }
    }

  private[this] def getPackagesInfoInGooglePlay(packages: List[Package], auth: MarketCredentials) =
    googleApi.getBulkDetails(packages, auth) map {
      case Left(_) ⇒
        ResolveMany.Response(Nil, packages, Nil)
      case Right(apps) ⇒
        val notFound = packages.diff(apps.map(_.packageName))
        ResolveMany.Response(notFound, Nil, apps)
    }

  def resolvePendingApps(numApps: Int): Free[F, ResolvePending.Response] = {
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
        for (status ← resolvePendingPackage(pack)) yield (pack, status)
      }
    } yield splitStatus(status)

  }

  def resolvePackageList(
    packages: List[Package],
    auth: MarketCredentials
  ): Free[F, ResolvePackagesResult] = {

    def splitResults(
      packages: List[Package],
      results: List[ApiFailure Either FullCard]
    ): (List[FullCard], List[Package], List[Package]) = {

      def classifyFailure(pack: Package, failure: ApiFailure): Either[Package, Package] =
        failure match {
          case ApiNotFound(_) ⇒ Right(pack)
          case _ ⇒ Left(pack)
        }

      def tupleLeft[A, B, C](a: A, either: Either[B, C]): Either[(A, B), C] =
        either.leftMap((b: B) ⇒ (a, b))

      val (labelFailures, cards): (List[(Package, ApiFailure)], List[FullCard]) =
        packages.zip(results)
          .map(Function.tupled(tupleLeft[Package, ApiFailure, FullCard]))
          .separate

      val (unknown, notFound) = labelFailures.map(Function.tupled(classifyFailure)).separate

      (cards, notFound, unknown)
    }

    /*
     * Here are two issues to challenge:
     *   1 - We must detect if there are and abort the rest.
     *   2 - we must make operations in parallel.
     * 
     * Now, all of these operations are made in a same monadic type 'F[_]', so we can
     * not use Task-specific or Task.Parallel specific operations, or conversions. 
     */
    for {
      cachedPackages ← cacheService.getValidMany(packages)
      uncachedPackages = packages diff cachedPackages.map(_.packageName)
      detailsResp ← googleApi.getDetailsList(uncachedPackages, auth)
      (cards, notFound, error) = splitResults(packages, detailsResp)
      _ ← cacheService.putResolvedMany(cards)
      _ ← cacheService.addErrorMany(notFound)
      _ ← cacheService.setToPendingMany(error)
    } yield ResolvePackagesResult(cachedPackages, cards, notFound, error)
  }

  def resolveNewPackage(pack: Package, auth: MarketCredentials): Free[F, getcard.Response] = {

    import getcard._

    // Third step: handle error and ask for package in Google Play
    def handleFailedResponse(failed: ApiFailure): Free[F, FailedResponse] =
      // Does package exists in Google Play?
      webScrapper.existsApp(pack) flatMap {
        case true ⇒ cacheService.setToPending(pack).map(_ ⇒ PendingResolution(pack))
        case false ⇒ cacheService.addError(pack).map(_ ⇒ UnknownPackage(pack))
      }

    // "Resolved or permanent Item in Redis Cache?"
    cacheService.getValid(pack) flatMap {
      case Some(card) ⇒
        // Yes -> Return the stored content
        Free.pure(Either.right(card))
      case None ⇒
        // No -> Google Play API Returns valid response?
        googleApi.getDetails(pack, auth) flatMap {
          case Right(card) ⇒
            // Yes -> Create key/value in Redis as Resolved, Return package info
            cacheService.putResolved(card).map(x ⇒ Either.right(card))
          case Left(apiFailure) ⇒
            handleFailedResponse(apiFailure).map(Either.left)
        }
    }
  }

  def resolvePendingPackage(pack: Package): Free[F, ResolvePending.PackageStatus] = {
    import ResolvePending._

    webScrapper.getDetails(pack) flatMap {
      case Right(card) ⇒
        cacheService.putResolved(card).map(x ⇒ Resolved)
      case Left(PageParseFailed(_)) ⇒
        cacheService.setToPending(pack).map(x ⇒ Pending)
      case Left(PackageNotFound(_)) ⇒
        cacheService.addError(pack).map(x ⇒ Unknown)
      case Left(WebPageServerError) ⇒
        cacheService.setToPending(pack).map(x ⇒ Pending)
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

