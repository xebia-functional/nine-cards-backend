package cards.nine.processes

import java.sql.Timestamp
import java.time.Instant

import cards.nine.commons.NineCardsErrors.{ CountryNotFound, RankingNotFound }
import cards.nine.commons.NineCardsService
import cards.nine.domain.account.{ AndroidId, DeviceToken }
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ FullCard, FullCardList, Package, Widget }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.processes.NineCardsServices.NineCardsServices
import cards.nine.processes.ProcessesExceptions.SharedCollectionNotFoundException
import cards.nine.processes.converters.Converters
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.services.free.domain.Firebase.{ NotificationIndividualResult, NotificationResponse }
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cards.nine.services.free.domain.{ SharedCollection ⇒ SharedCollectionServices, _ }
import cards.nine.services.free.interpreter.collection.Services.{ SharedCollectionData ⇒ SharedCollectionDataServices }
import org.joda.time.{ DateTime, DateTimeZone }

object TestData {

  val addedPackagesCount = 2

  val androidId = AndroidId("50a4dbf7-85a2-4875-8c75-7232c237808c")

  val appCategory = "COUNTRY"

  val author = "John Doe"

  val canonicalId = 0

  val category = "SOCIAL"

  val collectionId = 1l

  val community = true

  val countryContinent = "Americas"

  val countryIsoCode2 = "US"

  val countryIsoCode3 = "USA"

  val countryName = "United States"

  val deviceToken = DeviceToken("5d56922c-5257-4392-817e-503166cd7afd")

  val failure = 0

  val icon = "path-to-icon"

  val installationId = 10l

  val installations = 1

  val localization = Localization("en-EN")

  val messageId = "a000dcbd-5419-446f-b2c6-6eaefd88480c"

  val millis = 1453226400000l

  val multicastId = 15L

  val name = "The best social media apps"

  val pageNumber = 0

  val pageSize = 25

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val publishedOnDatetime = new DateTime(millis)

  val publishedOnTimestamp = Timestamp.from(Instant.ofEpochMilli(millis))

  val publisherId = 27L

  val removedPackagesCount = 1

  val stars = 5.0d

  val subscriberId = 42L

  val subscriptionsCount = 5L

  val success = 1

  val token = "6d54dfed-bbcf-47a5-b8f2-d86cf3320631"

  val updatedCollectionsCount = 1

  val userId = Option(publisherId)

  val views = 1

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val missing = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom"
  ) map Package

  val updatePackagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany"
  ) map Package

  val addedPackages = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom"
  ) map Package

  val removedPackages = List(
    "earth.europe.germany"
  ) map Package

  val updatedPackages = (addedPackages, removedPackages)

  object Values {

    val apps = packagesName map { packageName ⇒
      FullCard(
        packageName = packageName,
        title       = "Germany",
        free        = true,
        icon        = icon,
        stars       = stars,
        downloads   = "100.000+",
        categories  = List(appCategory),
        screenshots = Nil
      )
    }

    val appsInfo = FullCardList(missing, apps)

    val marketAuth = MarketCredentials(
      androidId    = androidId,
      localization = Some(localization),
      token        = MarketToken(token)
    )

    val collection = SharedCollectionServices(
      id               = collectionId,
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnTimestamp,
      author           = author,
      name             = name,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community
    )

    val collectionWithSubscriptions = SharedCollectionWithAggregatedInfo(
      sharedCollectionData = collection,
      subscriptionsCount   = subscriptionsCount
    )

    val country = Country(
      isoCode2  = countryIsoCode2,
      isoCode3  = Option(countryIsoCode3),
      name      = countryName,
      continent = countryContinent
    )

    val countryNotFoundError = CountryNotFound(s"Country with ISO code2 US doesn't exist")

    val nonExistentSharedCollection: Option[SharedCollectionServices] = None

    val packages = packagesName.zip(1l to packagesName.size.toLong) map {
      case (n, id) ⇒
        SharedCollectionPackage(id, collectionId, n.value)
    }

    val createPackagesStats = PackagesStats(addedPackagesCount, None)

    val updatePackagesStats = PackagesStats(addedPackagesCount, Option(removedPackagesCount))

    val appInfoList = packagesName map { packageName ⇒
      FullCard(packageName, "Germany", List(appCategory), "100.000+", true, icon, Nil, stars)
    }

    val installation = Installation(
      id          = installationId,
      userId      = subscriberId,
      deviceToken = Option(deviceToken),
      androidId   = androidId
    )

    val notificationResponse = NotificationResponse(
      multicast_id  = multicastId,
      success       = success,
      failure       = failure,
      canonical_ids = canonicalId,
      results       = Option(
        List(NotificationIndividualResult(
          message_id      = Option(messageId),
          registration_id = None,
          error           = None
        ))
      )
    )

    val sharedCollectionDataServices = SharedCollectionDataServices(
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnTimestamp,
      author           = author,
      name             = name,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community
    )

    val sharedCollectionData = SharedCollectionData(
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnDatetime,
      author           = author,
      name             = name,
      installations    = Option(installations),
      views            = Option(views),
      category         = category,
      icon             = icon,
      community        = community
    )

    val sharedCollection = SharedCollection(
      publicIdentifier = publicIdentifier,
      publishedOn      = new DateTime(publishedOnTimestamp.getTime),
      author           = author,
      name             = name,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community,
      owned            = true,
      packages         = packagesName
    )

    val sharedCollectionWithSubscriptions = SharedCollection(
      publicIdentifier   = publicIdentifier,
      publishedOn        = new DateTime(publishedOnTimestamp.getTime),
      author             = author,
      name               = name,
      installations      = installations,
      views              = views,
      category           = category,
      icon               = icon,
      community          = community,
      owned              = true,
      packages           = packagesName,
      subscriptionsCount = Option(subscriptionsCount)
    )

    val sharedCollectionUpdateInfo = SharedCollectionUpdateInfo(
      title = name
    )

    val sharedCollectionWithAppsInfo = SharedCollectionWithAppsInfo(
      collection = sharedCollection,
      appsInfo   = appInfoList
    )

    val sharedCollectionWithAppsInfoAndSubscriptions = SharedCollectionWithAppsInfo(
      collection = sharedCollectionWithSubscriptions,
      appsInfo   = appInfoList
    )

    val subscription = SharedCollectionSubscription(
      sharedCollectionId       = collectionId,
      userId                   = subscriberId,
      sharedCollectionPublicId = publicIdentifier
    )

    val updatedSubscriptionsCount = 1
  }

  object Messages {

    import Values._

    val createCollectionRequest: CreateCollectionRequest = CreateCollectionRequest(
      collection = sharedCollectionData,
      packages   = packagesName
    )

    val createCollectionResponse = CreateOrUpdateCollectionResponse(
      publicIdentifier = publicIdentifier,
      packagesStats    = createPackagesStats
    )

    val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
      data = sharedCollectionWithAppsInfo
    )

  }

  object Exceptions {

    val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
      message = "The required shared collection doesn't exist"
    )
  }

  object rankings {

    lazy val limit = 2
    lazy val location = Option("US")
    lazy val moments = List("HOME", "NIGHT")
    lazy val widgetMoments = moments map Converters.toWidgetMoment
    lazy val params = RankingParams(DateRange(startDate, endDate), 5, AnalyticsToken("auth_token"))
    lazy val scope = CountryScope(CountryIsoCode("ES"))
    lazy val usaScope = CountryScope(CountryIsoCode("US"))
    lazy val startDate = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC)
    lazy val endDate = new DateTime(2016, 2, 1, 0, 0, DateTimeZone.UTC)
    lazy val googleAnalyticsRanking = GoogleAnalyticsRanking(
      Map("SOCIAL" → List(Package("socialite"), Package("socialist")))
    )

    val rankingNotFoundError = RankingNotFound("Ranking not found for the scope")

    val emptyRankedAppsMap = Map.empty[String, List[RankedApp]]

    val emptyRankedWidgetsMap = Map.empty[String, List[RankedWidget]]

    val emptyUnrankedAppsList = List.empty[Package]

    val emptyUnrankedAppsMap = Map.empty[String, List[Package]]

    val emptyRankedAppsList = List.empty[RankedApp]

    val countriesAMCategory = "CountriesA-M"

    val countriesNZCategory = "CountriesN-Z"

    val countriesAMList = List(
      "earth.europe.france",
      "earth.europe.germany",
      "earth.europe.italy"
    ) map Package

    val countriesNZList = List(
      "earth.europe.portugal",
      "earth.europe.spain",
      "earth.europe.unitedKingdom"
    ) map Package

    val unrankedAppsList = countriesAMList ++ countriesNZList

    val unrankedAppsMap = Map(
      countriesAMCategory → countriesAMList,
      countriesNZCategory → countriesNZList
    )

    def appsWithRanking(apps: List[Package], category: String) =
      apps.zipWithIndex.map {
        case (packageName: Package, rank: Int) ⇒
          RankedApp(packageName, category, Option(rank))
      }

    val rankingForAppsResponse = NineCardsService.right[NineCardsServices, List[RankedApp]] {
      appsWithRanking(countriesAMList, countriesAMCategory) ++
        appsWithRanking(countriesNZList, countriesNZCategory)
    }

    val rankingForAppsEmptyResponse = NineCardsService.right[NineCardsServices, List[RankedApp]] {
      Nil
    }

    def widgetsWithRanking(apps: List[Package], category: String) =
      apps.zipWithIndex.map {
        case (packageName: Package, rank: Int) ⇒
          RankedWidget(Widget(packageName, "className"), category, Option(rank))
      }

    val rankingForWidgetsResponse = NineCardsService.right[NineCardsServices, List[RankedWidget]] {
      widgetsWithRanking(countriesAMList, countriesAMCategory) ++
        widgetsWithRanking(countriesNZList, countriesNZCategory)
    }

    val rankingForWidgetsEmptyResponse = NineCardsService.right[NineCardsServices, List[RankedWidget]] {
      Nil
    }
  }

}
