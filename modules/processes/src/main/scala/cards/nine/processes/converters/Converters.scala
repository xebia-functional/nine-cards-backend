package cards.nine.processes.converters

import java.sql.Timestamp

import cards.nine.domain.analytics.{ RankedApp, UnrankedApp }
import cards.nine.domain.application.{ FullCard, FullCardList, Package }
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.messages.UserMessages.LoginResponse
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionWithAggregatedInfo, Installation ⇒ InstallationServices, SharedCollection ⇒ SharedCollectionServices, SharedCollectionSubscription ⇒ SharedCollectionSubscriptionServices, User ⇒ UserAppServices }
import cards.nine.services.free.interpreter.collection.Services.{ SharedCollectionData ⇒ SharedCollectionDataServices }
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

  def toSharedCollection: (BaseSharedCollection, List[Package], Long) ⇒ SharedCollection = {
    case (collection: SharedCollectionWithAggregatedInfo, packages, userId) ⇒
      toSharedCollection(collection.sharedCollectionData, packages, Option(collection.subscriptionsCount), userId)
    case (collection: SharedCollectionServices, packages, userId) ⇒
      toSharedCollection(collection, packages, None, userId)
  }

  def toSharedCollection(
    collection: SharedCollectionServices,
    packages: List[Package],
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
    appsInfo: List[FullCard]
  ): SharedCollectionWithAppsInfo =
    SharedCollectionWithAppsInfo(
      collection = collection,
      appsInfo   = appsInfo
    )

  def filterCategorized(info: FullCardList): FullCardList = {
    val (appsWithoutCategories, apps) = info.cards.partition(app ⇒ app.categories.isEmpty)
    FullCardList(
      missing = info.missing ++ appsWithoutCategories.map(_.packageName),
      cards   = apps
    )
  }

  def toGetSubscriptionsByUserResponse(subscriptions: List[SharedCollectionSubscriptionServices]) =
    GetSubscriptionsByUserResponse(
      subscriptions map (_.sharedCollectionPublicId)
    )

  def toUnrankedApp(category: String)(pack: Package) = UnrankedApp(pack, category)

  def toRankedApp(app: UnrankedApp) = RankedApp(app.packageName, app.category, None)
}
