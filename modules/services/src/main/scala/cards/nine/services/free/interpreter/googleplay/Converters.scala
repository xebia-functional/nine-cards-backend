package cards.nine.services.free.interpreter.googleplay

import cards.nine.domain.application.{ Category, CardList, FullCardList, Package, PriceFilter }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.{ getcard, ResolveMany }
import cats.instances.list._
import cats.syntax.monadCombine._

object Converters {

  def ommitMissing[A](cardsList: CardList[A]): CardList[A] = cardsList.copy(missing = Nil)

  def toSearchAppsRequest(
    query: String,
    excludePackages: List[Package],
    limit: Int
  ): SearchAppsRequest = SearchAppsRequest(query, excludePackages, limit)

  def toRecommendByAppsRequest(
    packages: List[Package],
    limitByApp: Int,
    excludedPackages: List[Package],
    limit: Int
  ): RecommendByAppsRequest =
    RecommendByAppsRequest(
      packages,
      limitByApp,
      excludedPackages,
      limit
    )

  def toRecommendByCategoryRequest(
    category: String,
    filter: PriceFilter,
    excludedPackages: List[Package],
    limit: Int
  ): RecommendByCategoryRequest =
    RecommendByCategoryRequest(
      Category.withName(category),
      filter,
      excludedPackages,
      limit
    )

  def toCardList[A](response: ResolveMany.Response[A]): CardList[A] =
    CardList(
      response.pending ++ response.notFound,
      response.apps
    )

  def toFullCardList(response: List[getcard.Response]): FullCardList = {
    val (errors, resolved) = response.separate

    FullCardList(
      errors map (_.packageName),
      resolved
    )
  }

}
