package cards.nine.services.free.interpreter.googleplay

import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ Category, Package }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.getcard.{ FailedResponse, UnknownPackage }
import cards.nine.googleplay.processes.{ CardsProcesses, Wiring }
import cards.nine.services.free.domain.GooglePlay.{ AppInfo, AppsInfo, Recommendation, Recommendations }
import cats.data.Xor
import cats.free.Free
import org.specs2.matcher.{ DisjunctionMatchers, Matcher, Matchers, XorMatchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class GooglePlayServicesSpec
  extends Specification
  with Matchers
  with Mockito
  with DisjunctionMatchers
  with XorMatchers {

  import TestData._

  def recommendationIsFreeMatcher(isFree: Boolean): Matcher[Recommendation] = { rec: Recommendation ⇒
    rec.free must_== isFree
  }

  object TestData {

    def appInfoFor(packageName: String) = AppInfo(
      packageName = Package(packageName),
      title       = s"Title of $packageName",
      free        = false,
      icon        = s"Icon of $packageName",
      stars       = 5.0,
      downloads   = s"Downloads of $packageName",
      categories  = List(s"Category 1 of $packageName", s"Category 2 of $packageName")
    )

    def fullCardFor(packageName: String) = FullCard(
      packageName = Package(packageName),
      title       = s"Title of $packageName",
      free        = false,
      icon        = s"Icon of $packageName",
      stars       = 5.0,
      downloads   = s"Downloads of $packageName",
      categories  = List(s"Category 1 of $packageName", s"Category 2 of $packageName"),
      screenshots = List(s"Screenshot 1 of $packageName", s"Screenshot 2 of $packageName")
    )

    def recommendationFor(packageName: String) = Recommendation(
      packageName = Package(packageName),
      title       = s"Title of $packageName",
      free        = false,
      icon        = s"Icon of $packageName",
      stars       = 5.0,
      downloads   = s"Downloads of $packageName",
      screenshots = List(s"Screenshot 1 of $packageName", s"Screenshot 2 of $packageName")
    )

    val packagesName = List("com.package.one", "com.package.two", "com.package.three", "com.package.four")
    val onePackageName = packagesName.head

    val packages = packagesName map Package
    val onePackage = Package(onePackageName)

    val (validPackagesName, wrongPackagesName) = packagesName.partition(_.length <= 15)

    val validPackages = validPackagesName map Package
    val wrongPackages = wrongPackagesName map Package

    val appInfoList = validPackagesName map appInfoFor

    val recommendations = validPackagesName map recommendationFor

    val category = "SHOPPING"

    val limit = 20

    val numPerApp = 25

    val priceFilter = "FREE"

    object AuthData {
      val androidId = "12345"
      val localization = "en_GB"
      val token = "m52_9876"

      val marketAuth = MarketCredentials(
        AndroidId(androidId),
        MarketToken(token),
        Some(Localization(localization))
      )

    }

    object Requests {
      val recommendByAppsRequest = RecommendByAppsRequest(
        searchByApps = packages,
        numPerApp    = numPerApp,
        excludedApps = wrongPackages,
        maxTotal     = limit
      )

      val recommendByCategoryRequest = RecommendByCategoryRequest(
        category     = Category.SHOPPING,
        priceFilter  = PriceFilter.FREE,
        excludedApps = wrongPackages,
        maxTotal     = limit
      )
    }

    object GooglePlayResponses {
      val fullCard = fullCardFor(onePackageName)
      val unknwonPackageError = UnknownPackage(onePackage)

      val fullCards = validPackagesName map fullCardFor
      val unknownPackageErrors = wrongPackages map UnknownPackage

      val recommendationsInfoError = InfoError("Something went wrong!")

      val fullCardList = FullCardList(
        missing = wrongPackages,
        cards   = fullCards
      )

      val getCardsResponse: List[Xor[FailedResponse, FullCard]] =
        (fullCards map Xor.right) ++ (unknownPackageErrors map Xor.left)
    }
  }

  trait BasicScope extends Scope {
    implicit val googlePlayProcesses = mock[CardsProcesses[Wiring.GooglePlayApp]]
    val services = Services.services
  }

  "resolveOne" should {
    "return the App object when a valid package name is provided" in new BasicScope {

      googlePlayProcesses.getCard(onePackage, AuthData.marketAuth) returns
        Free.pure(Xor.right(GooglePlayResponses.fullCard))

      val response = services.resolveOne(onePackage, AuthData.marketAuth)

      response.unsafePerformSyncAttempt must be_\/-[String Xor AppInfo].which {
        content ⇒ content must beXorRight[AppInfo]
      }
    }

    "return an error message when a wrong package name is provided" in new BasicScope {

      googlePlayProcesses.getCard(onePackage, AuthData.marketAuth) returns
        Free.pure(Xor.left(GooglePlayResponses.unknwonPackageError))

      val response = services.resolveOne(onePackage, AuthData.marketAuth)

      response.unsafePerformSyncAttempt must be_\/-[String Xor AppInfo].which {
        content ⇒ content must beXorLeft[String](onePackageName)
      }
    }
  }

  "resolveMany" should {
    "return the list of apps that are valid and those that are wrong" in new BasicScope {

      googlePlayProcesses.getCards(packages, AuthData.marketAuth) returns
        Free.pure(GooglePlayResponses.getCardsResponse)

      val response = services.resolveMany(packages, AuthData.marketAuth, true)

      response.unsafePerformSyncAttempt must be_\/-[AppsInfo].which { appsInfo ⇒
        appsInfo.missing must containTheSameElementsAs(wrongPackages)
        appsInfo.apps must containTheSameElementsAs(appInfoList)
      }
    }
  }

  "recommendByCategory" should {
    "return a list of free recommended apps for the given category" in new BasicScope {

      googlePlayProcesses.recommendationsByCategory(
        Requests.recommendByCategoryRequest,
        AuthData.marketAuth
      ) returns Free.pure(Xor.right(GooglePlayResponses.fullCardList))

      val response = services.recommendByCategory(
        category         = category,
        filter           = priceFilter,
        excludedPackages = wrongPackages,
        limit            = limit,
        auth             = AuthData.marketAuth
      )

      response.unsafePerformSyncAttempt must be_\/-[Recommendations].which { rec ⇒
        rec.apps must containTheSameElementsAs(recommendations)
      }
    }

    "fail if something went wrong while getting recommendations" in new BasicScope {

      googlePlayProcesses.recommendationsByCategory(
        Requests.recommendByCategoryRequest,
        AuthData.marketAuth
      ) returns Free.pure(Xor.left(GooglePlayResponses.recommendationsInfoError))

      val response = services.recommendByCategory(
        category         = category,
        filter           = priceFilter,
        excludedPackages = wrongPackages,
        limit            = limit,
        auth             = AuthData.marketAuth
      )

      // TODO: We shouldn't use Throwable to model this error
      response.unsafePerformSyncAttempt must be_-\/[Throwable]
    }

  }

  "recommendationsForApps" should {
    "return a list of recommended apps for the given list of packages" in new BasicScope {
      googlePlayProcesses.recommendationsByApps(
        Requests.recommendByAppsRequest,
        AuthData.marketAuth
      ) returns Free.pure(GooglePlayResponses.fullCardList)

      val response = services.recommendationsForApps(
        packageNames     = packages,
        excludedPackages = wrongPackages,
        limitByApp       = numPerApp,
        limit            = limit,
        auth             = AuthData.marketAuth
      )

      response.unsafePerformSyncAttempt must be_\/-[Recommendations].which { rec ⇒
        rec.apps must containTheSameElementsAs(recommendations)
      }
    }
  }
}
