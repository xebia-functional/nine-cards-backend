package cards.nine.services.free.interpreter.googleplay

import cards.nine.domain.application.Category
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.{ getcard, ResolveMany }
import cards.nine.services.free.domain.GooglePlay.{ RecommendByCategoryRequest ⇒ _, _ }
import cats.instances.list._
import cats.syntax.monadCombine._

object Converters {

  def toRecommendations(cardsList: FullCardList): Recommendations =
    Recommendations(
      cardsList.cards map toRecommendation
    )

  def toRecommendation(card: FullCard): Recommendation =
    Recommendation(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      screenshots = card.screenshots
    )

  def toSearchAppsRequest(
    query: String,
    excludePackages: List[String],
    limit: Int
  ): SearchAppsRequest = SearchAppsRequest(query, excludePackages, limit)

  def toRecommendByAppsRequest(
    packages: List[String],
    limitByApp: Int,
    excludedPackages: List[String],
    limit: Int
  ): RecommendByAppsRequest =
    RecommendByAppsRequest(
      packages map Package,
      limitByApp,
      excludedPackages map Package,
      limit
    )

  def toRecommendByCategoryRequest(
    category: String,
    filter: String,
    excludedPackages: List[String],
    limit: Int
  ): RecommendByCategoryRequest =
    RecommendByCategoryRequest(
      Category.withName(category),
      PriceFilter.withName(filter),
      excludedPackages map Package,
      limit
    )

  def toGoogleAuthParams(auth: AuthParams): GoogleAuthParams =
    GoogleAuthParams(
      AndroidId(auth.androidId),
      Token(auth.token),
      auth.localization map Localization.apply
    )

  def toAppsInfo(response: ResolveMany.Response): AppsInfo =
    AppsInfo(
      (response.pending ++ response.notFound) map (_.value),
      response.apps map toAppInfo
    )

  def toAppsInfo(response: List[getcard.Response]): AppsInfo = {
    val (errors, resolved) = response.separate

    AppsInfo(
      errors map (_.packageName.value),
      resolved map toAppInfo
    )
  }

  def toAppInfo(card: FullCard): AppInfo =
    AppInfo(
      packageName = card.packageName,
      title       = card.title,
      free        = card.free,
      icon        = card.icon,
      stars       = card.stars,
      downloads   = card.downloads,
      categories  = card.categories
    )
}
