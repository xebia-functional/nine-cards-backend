package cards.nine.processes.collections

import java.sql.Timestamp

import cards.nine.domain.application.{ CardList, FullCard, Moment, Package }
import cards.nine.processes.collections.messages._
import cards.nine.services.free.domain.{ BaseSharedCollection, SharedCollectionWithAggregatedInfo, SharedCollection ⇒ SharedCollectionServices, SharedCollectionSubscription ⇒ SharedCollectionSubscriptionServices }
import cards.nine.services.free.interpreter.collection.Services.{ SharedCollectionData ⇒ SharedCollectionDataServices }
import org.joda.time.DateTime

private[collections] object Converters {

  implicit def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  implicit def toTimestamp(datetime: DateTime): Timestamp = new Timestamp(datetime.getMillis)

  implicit def toSharedCollectionDataServices(
    data: SharedCollectionData
  ): SharedCollectionDataServices =
    SharedCollectionDataServices(
      publicIdentifier = data.publicIdentifier,
      userId           = data.userId,
      publishedOn      = data.publishedOn,
      author           = data.author,
      name             = data.name,
      views            = data.views.getOrElse(0),
      category         = data.category,
      icon             = data.icon,
      community        = data.community,
      packages         = data.packages map (_.value)
    )

  def toSharedCollectionList(userId: Long)(collections: List[BaseSharedCollection]): List[SharedCollection] =
    collections map (col ⇒ toSharedCollection(col, userId))

  def toSharedCollection(collection: BaseSharedCollection, userId: Long): SharedCollection =
    collection match {
      case (c: SharedCollectionWithAggregatedInfo) ⇒
        toSharedCollection(c.sharedCollectionData, Option(c.subscriptionsCount), userId)
      case (c: SharedCollectionServices) ⇒
        toSharedCollection(c, None, userId)
    }

  def toSharedCollection(
    collection: SharedCollectionServices,
    subscriptionCount: Option[Long],
    userId: Long
  ) =
    SharedCollection(
      publicIdentifier   = collection.publicIdentifier,
      publishedOn        = collection.publishedOn,
      author             = collection.author,
      name               = collection.name,
      views              = collection.views,
      category           = collection.category,
      icon               = collection.icon,
      community          = collection.community,
      owned              = collection.userId.fold(false)(user ⇒ user == userId),
      packages           = collection.packages map Package,
      subscriptionsCount = subscriptionCount
    )

  def toSharedCollectionWithAppsInfo[A](
    collection: SharedCollection,
    appsInfo: List[A]
  ): SharedCollectionWithAppsInfo[A] =
    SharedCollectionWithAppsInfo(
      collection = collection,
      appsInfo   = appsInfo
    )

  def filterCategorized(info: CardList[FullCard]): CardList[FullCard] = {
    val (appsWithoutCategories, apps) = info.cards.partition(app ⇒ app.categories.isEmpty)
    CardList(
      missing = info.missing ++ appsWithoutCategories.map(_.packageName),
      cards   = apps
    )
  }

  def toGetSubscriptionsByUserResponse(subscriptions: List[SharedCollectionSubscriptionServices]) =
    GetSubscriptionsByUserResponse(
      subscriptions map (_.sharedCollectionPublicId)
    )

  def toMoment(widgetMoment: String) = widgetMoment.replace(Moment.widgetMomentPrefix, "")

}
