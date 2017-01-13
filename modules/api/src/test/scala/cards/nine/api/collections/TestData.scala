package cards.nine.api.collections

import cards.nine.api.collections.messages._
import cards.nine.commons.NineCardsErrors.SharedCollectionNotFound
import cards.nine.domain.application.Package
import cards.nine.processes.collections.messages._
import org.joda.time.DateTime

private[collections] object TestData {

  val addedPackages = 5

  val author = "John Doe"

  val category = "SOCIAL"

  val community = true

  val icon = "path-to-icon"

  val installations = 1

  val location = Option("US")

  val marketLocalization = "en-us"

  val name = "The best social media apps"

  val now = DateTime.now

  val owned = true

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val deviceApps = Map("countries" → packagesName)

  val excludePackages = packagesName.filter(_.value.length > 18)

  val moments = List("HOME", "NIGHT")

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val removedPackages = None

  val views = 1

  val sharedCollectionNotFoundError = SharedCollectionNotFound("Shared collection not found")

  object Exceptions {

    val http4sException = org.http4s.InvalidResponseException(msg = "Test error")

  }

  object Messages {

    val collectionInfo = SharedCollectionUpdateInfo(title = name)

    val packagesStats = PackagesStats(addedPackages, removedPackages)

    val sharedCollection = SharedCollection(
      publicIdentifier = publicIdentifier,
      publishedOn      = now,
      author           = author,
      name             = name,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community,
      owned            = owned,
      packages         = packagesName
    )

    def sharedCollectionInfo[A] = SharedCollectionWithAppsInfo[A](
      collection = sharedCollection,
      appsInfo   = List.empty[A]
    )

    val apiCreateCollectionRequest = ApiCreateCollectionRequest(
      author        = author,
      name          = name,
      installations = Option(installations),
      views         = Option(views),
      category      = category,
      icon          = icon,
      community     = community,
      packages      = packagesName
    )

    val apiUpdateCollectionRequest = ApiUpdateCollectionRequest(
      collectionInfo = Option(collectionInfo),
      packages       = Option(packagesName)
    )

    val createOrUpdateCollectionResponse = CreateOrUpdateCollectionResponse(
      publicIdentifier = publicIdentifier,
      packagesStats    = packagesStats
    )

    val increaseViewsCountByOneResponse = IncreaseViewsCountByOneResponse(
      publicIdentifier = publicIdentifier
    )

    val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
      data = sharedCollectionInfo
    )

    val getCollectionsResponse = GetCollectionsResponse(Nil)

    val getSubscriptionsByUserResponse = GetSubscriptionsByUserResponse(List(publicIdentifier))

    val subscribeResponse = SubscribeResponse()

    val unsubscribeResponse = UnsubscribeResponse()

  }

  object Paths {

    val collections = "/collections"

    val collectionById = "/collections/40daf308-fecf-4228-9262-a712d783cf49"

    val increaseViews = "/collections/40daf308-fecf-4228-9262-a712d783cf49/views"

    val latestCollections = "/collections/latest/SOCIAL/0/25"

    val subscriptionByCollectionId = "/collections/subscriptions/40daf308-fecf-4228-9262-a712d783cf49"

    val subscriptionsByUser = "/collections/subscriptions"

    val topCollections = "/collections/top/SOCIAL/0/25"
  }

}
