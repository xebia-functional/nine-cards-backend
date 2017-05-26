/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.processes

import cats.instances.either._
import cats.instances.list._
import cats.syntax.monadCombine._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.syntax.either._
import cards.nine.domain.application.{ BasicCard, CardList, FullCard, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle.{ ResolvePackagesResult, Failure ⇒ ApiFailure, PackageNotFound ⇒ ApiNotFound }
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import freestyle._
import freestyle.implicits._

class CardsProcesses[F[_]](
  googleApi: GoogleApi[F],
  cacheService: Cache[F],
  webScrapper: WebScraper[F]
) {

  def getBasicCards(
    packs: List[Package],
    auth: MarketCredentials
  ): FreeS[F, ResolveMany.Response[BasicCard]] =
    for {
      cached ← cacheService.getValidMany(packs).freeS
      uncached = packs diff (cached map (_.packageName))
      response ← getPackagesInfoInGooglePlay(uncached, auth).freeS
    } yield ResolveMany.Response(response.notFound, response.pending, cached.map(_.toBasic) ++ response.apps)

  def getCard(pack: Package, auth: MarketCredentials): FreeS[F, getcard.Response] =
    resolveNewPackage(pack, auth)

  def getCards(packages: List[Package], auth: MarketCredentials): FreeS[F, ResolveMany.Response[FullCard]] =
    for {
      result ← resolvePackageList(packages, auth)
    } yield ResolveMany.Response(
      notFound = result.notFoundPackages,
      pending  = result.pendingPackages,
      apps     = result.resolvedPackages ++ result.cachedPackages
    )

  def storeCard(card: FullCard): FreeS[F, Unit] =
    cacheService.putPermanent(card).freeS

  def recommendationsByCategory(
    request: RecommendByCategoryRequest,
    auth: MarketCredentials
  ): FreeS[F, InfoError Either CardList[FullCard]] =
    googleApi.recommendationsByCategory(request, auth).freeS flatMap {
      case Left(error) ⇒ FreeS.pure(Either.left(error))
      case Right(recommendations) ⇒
        val packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
        for {
          result ← resolvePackageList(packages, auth)
        } yield Either.right(CardList(
          missing = result.notFoundPackages,
          pending = result.pendingPackages,
          cards   = result.cachedPackages ++ result.resolvedPackages
        ))
    }

  def recommendationsByApps(
    request: RecommendByAppsRequest,
    auth: MarketCredentials
  ): FreeS[F, CardList[FullCard]] =
    for {
      recommendations ← googleApi.recommendationsByApps(request, auth)
      packages = recommendations.diff(request.excludedApps).take(request.maxTotal)
      result ← resolvePackageList(packages, auth)
    } yield CardList(
      missing = result.notFoundPackages,
      pending = result.pendingPackages,
      cards   = result.cachedPackages ++ result.resolvedPackages
    )

  def searchApps(request: SearchAppsRequest, auth: MarketCredentials): FreeS[F, CardList[BasicCard]] =
    googleApi.searchApps(request, auth) flatMap {
      case Left(_) ⇒ FreeS.pure(CardList(Nil, Nil, Nil))
      case Right(packs) ⇒
        getBasicCards(packs, auth) map { r ⇒
          CardList(
            missing = r.notFound,
            pending = r.pending,
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

  def resolvePendingApps(numApps: Int): FreeS[F, ResolvePending.Response] = {
    import ResolvePending._

    def splitStatus(status: List[(Package, PackageStatus)]): Response = {
      val solved = status collect { case (pack, Resolved) ⇒ pack }
      val unknown = status collect { case (pack, Unknown) ⇒ pack }
      val pending = status collect { case (pack, Pending) ⇒ pack }
      Response(solved, unknown, pending)
    }

    for {
      list ← cacheService.listPending(numApps).freeS
      status ← list.traverse[FreeS[F, ?], (Package, PackageStatus)] { pack ⇒
        for (status ← resolvePendingPackage(pack)) yield (pack, status)
      }
    } yield splitStatus(status)

  }

  def resolvePackageList(packages: List[Package], auth: MarketCredentials): FreeS[F, ResolvePackagesResult] = {

    def splitResults(
      packages: List[Package],
      results: List[ApiFailure Either FullCard]
    ): (List[FullCard], List[Package], List[Package]) = {

      def classifyFailure(pack: Package, failure: ApiFailure): Either[Package, Package] =
        failure match {
          case ApiNotFound(_) ⇒ Right(pack)
          case _ ⇒ Left(pack)
        }

      val (labelFailures, cards): (List[(Package, ApiFailure)], List[FullCard]) =
        packages.zip(results)
          .map({ case (pack, eith) ⇒ eith.leftMap(apif ⇒ (pack, apif)) })
          .separate

      val (unknown, notFound) = labelFailures.map(Function.tupled(classifyFailure)).separate

      (cards, notFound, unknown)
    }

    for {
      cachedPackages ← cacheService.getValidMany(packages).freeS
      uncachedPackages = packages diff cachedPackages.map(_.packageName)
      detailsResp ← googleApi.getDetailsList(uncachedPackages, auth).freeS
      (cards, notFound, error) = splitResults(packages, detailsResp)
      _ ← cacheService.putResolvedMany(cards).freeS
      _ ← cacheService.addErrorMany(notFound).freeS
      _ ← cacheService.setToPendingMany(error).freeS
    } yield ResolvePackagesResult(cachedPackages, cards, notFound, error)
  }

  def resolveNewPackage(pack: Package, auth: MarketCredentials): FreeS[F, getcard.Response] = {

    import getcard._

    // Third step: handle error and ask for package in Google Play
    def handleFailedResponse(failed: ApiFailure): FreeS[F, FailedResponse] =
      // Does package exists in Google Play?
      webScrapper.existsApp(pack) flatMap {
        case true ⇒
          cacheService.setToPending(pack).as(PendingResolution(pack): FailedResponse).freeS
        case false ⇒
          cacheService.addError(pack).as(UnknownPackage(pack): FailedResponse).freeS
      }

    // "Resolved or permanent Item in Redis Cache?"
    cacheService.getValid(pack).freeS flatMap {
      case Some(card) ⇒
        // Yes -> Return the stored content
        FreeS.pure(Either.right(card))
      case None ⇒
        // No -> Google Play API Returns valid response?
        googleApi.getDetails(pack, auth).freeS flatMap {
          case Right(card) ⇒
            // Yes -> Create key/value in Redis as Resolved, Return package info
            cacheService.putResolved(card).as(Either.right(card): getcard.Response).freeS
          case Left(apiFailure) ⇒
            handleFailedResponse(apiFailure).map(Either.left)
        }
    }
  }

  def resolvePendingPackage(pack: Package): FreeS[F, ResolvePending.PackageStatus] = {
    import ResolvePending._

    webScrapper.getDetails(pack) flatMap {
      case Right(card) ⇒
        cacheService.putResolved(card).as(Resolved: PackageStatus).freeS
      case Left(PageParseFailed(_)) ⇒
        cacheService.setToPending(pack).as(Pending: PackageStatus).freeS
      case Left(PackageNotFound(_)) ⇒
        cacheService.addError(pack).as(Unknown: PackageStatus).freeS
      case Left(WebPageServerError) ⇒
        cacheService.setToPending(pack).as(Pending: PackageStatus).freeS
    }
  }

}

object CardsProcesses {

  implicit def processes[F[_]](
    implicit
    apiS: GoogleApi[F],
    cacheS: Cache[F],
    webS: WebScraper[F]
  ): CardsProcesses[F] = new CardsProcesses(apiS, cacheS, webS)
}

