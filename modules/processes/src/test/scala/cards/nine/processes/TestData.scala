package cards.nine.processes

import java.sql.Timestamp
import java.time.Instant

import cards.nine.commons.NineCardsErrors.CountryNotFound
import cards.nine.processes.ProcessesExceptions.SharedCollectionNotFoundException
import cards.nine.processes.messages.GooglePlayAuthMessages.AuthParams
import cards.nine.processes.messages.SharedCollectionMessages._
import cards.nine.processes.messages.rankings.GetRankedDeviceApps.DeviceApp
import cards.nine.services.free.domain.{ SharedCollection ⇒ SharedCollectionServices, _ }
import cards.nine.services.free.domain.Firebase.{ NotificationIndividualResult, NotificationResponse }
import cards.nine.services.free.domain.GooglePlay.{ AppsInfo, AppInfo ⇒ AppInfoServices }
import cards.nine.services.free.domain.rankings.{ Country ⇒ CountryEnum }
import cards.nine.services.free.interpreter.collection.Services.{ SharedCollectionData ⇒ SharedCollectionDataServices }
import org.joda.time.{ DateTime, DateTimeZone }

object TestData {

  val addedPackagesCount = 2

  val androidId = "50a4dbf7-85a2-4875-8c75-7232c237808c"

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

  val deviceToken = "5d56922c-5257-4392-817e-503166cd7afd"

  val failure = 0

  val icon = "path-to-icon"

  val installationId = 10l

  val installations = 1

  val localization = Option("en-EN")

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
  )

  val missing = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom"
  )

  val updatePackagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany"
  )

  val addedPackages = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom"
  )

  val removedPackages = List(
    "earth.europe.germany"
  )

  val updatedPackages = (addedPackages, removedPackages)

  object Values {

    val apps = packagesName map { packageName ⇒
      AppInfoServices(packageName, "Germany", true, icon, stars, "100.000+", List(appCategory))
    }

    val appsInfo = AppsInfo(missing, apps)

    val authParams = AuthParams(
      androidId    = androidId,
      localization = localization,
      token        = token
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
        SharedCollectionPackage(id, collectionId, n)
    }

    val createPackagesStats = PackagesStats(addedPackagesCount, None)

    val updatePackagesStats = PackagesStats(addedPackagesCount, Option(removedPackagesCount))

    val appInfoList = packagesName map { packageName ⇒
      AppInfo(packageName, "Germany", true, icon, stars, "100.000+", appCategory)
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
    import cards.nine.services.free.domain.rankings.{ AuthParams ⇒ AuthRanking, _ }

    lazy val scope = CountryScope(CountryEnum.Spain)
    lazy val location = Option("US")
    lazy val startDate = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC)
    lazy val endDate = new DateTime(2016, 2, 1, 0, 0, DateTimeZone.UTC)
    lazy val params = RankingParams(DateRange(startDate, endDate), 5, AuthRanking("auth_token"))
    lazy val error = RankingError(401, "Unauthorized", "Unauthorized")
    lazy val ranking = Ranking(Map(Category.SOCIAL →
      CategoryRanking(List(PackageName("socialite"), PackageName("socialist")))))

    val emptyDeviceAppsMap = Map.empty[String, List[DeviceApp]]

    val emptyRankedAppsList = List.empty[RankedApp]

    val countriesAMCategory = "CountriesA-M"

    val countriesNZCategory = "CountriesN-Z"

    val countriesAMList = List(
      "earth.europe.france",
      "earth.europe.germany",
      "earth.europe.italy"
    )

    val countriesNZList = List(
      "earth.europe.portugal",
      "earth.europe.spain",
      "earth.europe.unitedKingdom"
    )

    val deviceAppsMap = Map(
      countriesAMCategory → countriesAMList.map(DeviceApp.apply),
      countriesNZCategory → countriesNZList.map(DeviceApp.apply)
    )

    def appsWithRanking(apps: List[String], category: String) =
      apps.zipWithIndex.map {
        case (packageName: String, rank: Int) ⇒
          RankedApp(packageName, category, Option(rank))
      }

    val rankedAppsList =
      appsWithRanking(countriesAMList, countriesAMCategory) ++
        appsWithRanking(countriesNZList, countriesNZCategory)
  }
}
