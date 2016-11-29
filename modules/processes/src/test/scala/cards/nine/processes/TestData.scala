package cards.nine.processes

import java.sql.Timestamp
import java.time.Instant
import cards.nine.commons.NineCardsErrors.{ CountryNotFound, RankingNotFound }
import cards.nine.commons.NineCardsService
import cards.nine.domain.account.{ AndroidId, DeviceToken }
import cards.nine.domain.analytics._
import cards.nine.domain.application.{ CardList, FullCard, Package, Widget }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.domain.pagination.Page
import cards.nine.processes.NineCardsServices.NineCardsServices
import cards.nine.processes.converters.Converters
import cards.nine.services.free.domain.Ranking.GoogleAnalyticsRanking
import cards.nine.services.free.domain.{ Country, Installation }
import org.joda.time.{ DateTime, DateTimeZone }

object TestData {

  val addedPackagesCount = 2

  val androidId = AndroidId("50a4dbf7-85a2-4875-8c75-7232c237808c")

  val appCategory = "COUNTRY"

  val author = "John Doe"

  val canonicalId = 0

  val category = "SOCIAL"

  val community = true

  val countryContinent = "Americas"

  val countryIsoCode2 = "US"

  val countryIsoCode3 = "USA"

  val countryName = "United States"

  val deviceToken = DeviceToken("5d56922c-5257-4392-817e-503166cd7afd")

  val failure = 0

  val icon = "path-to-icon"

  val installationId = 10l

  val localization = Localization("en-EN")

  val messageId = "a000dcbd-5419-446f-b2c6-6eaefd88480c"

  val millis = 1453226400000l

  val multicastId = 15L

  val name = "The best social media apps"

  val pageNumber = 0

  val pageSize = 25

  val pageParams = Page(pageNumber, pageSize)

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

    val appsInfo = CardList(missing, apps)
    val appsInfoBasic = CardList(missing, apps.map(_.toBasic))

    val marketAuth = MarketCredentials(
      androidId    = androidId,
      localization = Some(localization),
      token        = MarketToken(token)
    )

    val country = Country(
      isoCode2  = countryIsoCode2,
      isoCode3  = Option(countryIsoCode3),
      name      = countryName,
      continent = countryContinent
    )

    val countryNotFoundError = CountryNotFound(s"Country with ISO code2 US doesn't exist")

    val appInfoList = packagesName map { packageName ⇒
      FullCard(packageName, "Germany", List(appCategory), "100.000+", true, icon, Nil, stars)
    }

    val installation = Installation(
      id          = installationId,
      userId      = subscriberId,
      deviceToken = Option(deviceToken),
      androidId   = androidId
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
