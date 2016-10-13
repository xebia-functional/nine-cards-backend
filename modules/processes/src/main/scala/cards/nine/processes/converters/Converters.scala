package cards.nine.processes.converters

import java.sql.Timestamp

import cards.nine.processes.messages.ApplicationMessages._
import cards.nine.processes.messages.GooglePlayAuthMessages._
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps._
import cards.nine.processes.messages.RecommendationsMessages._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.messages.UserMessages.LoginResponse
import cards.nine.services.free.domain.GooglePlay.{
  AppsInfo,
  AppInfo ⇒ AppInfoServices,
  AuthParams ⇒ AuthParamServices,
  Recommendation,
  Recommendations
}
import cards.nine.services.free.domain.rankings.{ RankedApp, UnrankedApp }
import cards.nine.services.free.domain.{
  BaseSharedCollection,
  Installation ⇒ InstallationServices,
  SharedCollection ⇒ SharedCollectionServices,
  SharedCollectionSubscription ⇒ SharedCollectionSubscriptionServices,
  SharedCollectionWithAggregatedInfo,
  User ⇒ UserAppServices
}
import cards.nine.services.free.interpreter.collection.Services.{
  SharedCollectionData ⇒ SharedCollectionDataServices
}

import org.joda.time.DateTime

object Converters {

  implicit def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  implicit def toTimestamp(datetime: DateTime): Timestamp = new Timestamp(datetime.getMillis)

  def toLoginResponse(info: (UserAppServices, InstallationServices)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      apiKey       = user.apiKey,
      sessionToken = user.sessionToken
    )
  }

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId   = installation.androidId,
      deviceToken = installation.deviceToken
    )

  implicit def toSharedCollectionDataServices(
    data: SharedCollectionData
  ): SharedCollectionDataServices =
    SharedCollectionDataServices(
      publicIdentifier = data.publicIdentifier,
      userId           = data.userId,
      publishedOn      = data.publishedOn,
      author           = data.author,
      name             = data.name,
      installations    = data.installations.getOrElse(0),
      views            = data.views.getOrElse(0),
      category         = data.category,
      icon             = data.icon,
      community        = data.community
    )

  def toSharedCollection: (BaseSharedCollection, List[String], Long) ⇒ SharedCollection = {
    case (collection: SharedCollectionWithAggregatedInfo, packages, userId) ⇒
      toSharedCollection(collection.sharedCollectionData, packages, Option(collection.subscriptionsCount), userId)
    case (collection: SharedCollectionServices, packages, userId) ⇒
      toSharedCollection(collection, packages, None, userId)
  }

  def toSharedCollection(
    collection: SharedCollectionServices,
    packages: List[String],
    subscriptionCount: Option[Long],
    userId: Long
  ) =
    SharedCollection(
      publicIdentifier   = collection.publicIdentifier,
      publishedOn        = collection.publishedOn,
      author             = collection.author,
      name               = collection.name,
      installations      = collection.installations,
      views              = collection.views,
      category           = collection.category,
      icon               = collection.icon,
      community          = collection.community,
      owned              = collection.userId.fold(false)(user ⇒ user == userId),
      packages           = packages,
      subscriptionsCount = subscriptionCount
    )

  def toSharedCollectionWithAppsInfo(
    collection: SharedCollection,
    appsInfo: List[AppInfoServices]
  ): SharedCollectionWithAppsInfo =
    SharedCollectionWithAppsInfo(
      collection = collection,
      appsInfo   = appsInfo map toAppInfo
    )

  def toAppInfo(info: AppInfoServices): AppInfo =
    AppInfo(
      packageName = info.packageName,
      title       = info.title,
      free        = info.free,
      icon        = info.icon,
      stars       = info.stars,
      downloads   = info.downloads,
      category    = info.categories.headOption getOrElse ""
    )

  def toGetAppsInfoResponse(info: AppsInfo): GetAppsInfoResponse = {
    val (appsWithoutCategories, apps) = info.apps.partition(app ⇒ app.categories.isEmpty)

    GetAppsInfoResponse(
      errors = info.missing ++ appsWithoutCategories.map(_.packageName),
      items  = apps map { app ⇒
      AppGooglePlayInfo(
        packageName = app.packageName,
        title       = app.title,
        free        = app.free,
        icon        = app.icon,
        stars       = app.stars,
        downloads   = app.downloads,
        categories  = app.categories
      )
    }
    )
  }

  def toGooglePlayRecommendation(recommendation: Recommendation): GooglePlayRecommendation =
    GooglePlayRecommendation(
      packageName = recommendation.packageName,
      title       = recommendation.title,
      free        = recommendation.free,
      icon        = recommendation.icon,
      stars       = recommendation.stars,
      downloads   = recommendation.downloads,
      screenshots = recommendation.screenshots
    )

  def toGetRecommendationsResponse(recommendations: Recommendations): GetRecommendationsResponse =
    GetRecommendationsResponse(
      items = recommendations.apps map toGooglePlayRecommendation
    )

  def toAuthParamsServices(authParams: AuthParams): AuthParamServices = {
    AuthParamServices(
      androidId    = authParams.androidId,
      localization = authParams.localization,
      token        = authParams.token
    )
  }

  def toGetSubscriptionsByUserResponse(subscriptions: List[SharedCollectionSubscriptionServices]) =
    GetSubscriptionsByUserResponse(
      subscriptions map (_.sharedCollectionPublicId)
    )

  def toUnrankedApp(category: String)(app: DeviceApp) = UnrankedApp(app.packageName, category)

  def toRankedDeviceApp(app: RankedApp) = RankedDeviceApp(app.packageName, app.ranking)
}
