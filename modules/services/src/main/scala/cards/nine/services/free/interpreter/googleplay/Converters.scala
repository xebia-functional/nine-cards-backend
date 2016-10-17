package cards.nine.services.free.interpreter.googleplay

import cards.nine.domain.application.{ Category, FullCardList, Package }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.{ getcard, ResolveMany }
import cats.instances.list._
import cats.syntax.monadCombine._

object Converters {

  def toRecommendations(cardsList: FullCardList): FullCardList =
    FullCardList(Nil, cardsList.cards)

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
    filter: String,
    excludedPackages: List[Package],
    limit: Int
  ): RecommendByCategoryRequest =
    RecommendByCategoryRequest(
      Category.withName(category),
      PriceFilter.withName(filter),
      excludedPackages,
      limit
    )

  def toFullCardList(response: ResolveMany.Response): FullCardList =
    FullCardList(
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
