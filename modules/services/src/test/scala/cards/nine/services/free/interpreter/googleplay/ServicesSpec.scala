package cards.nine.services.free.interpreter.googleplay

import akka.actor.ActorSystem
import cards.nine.commons.NineCardsErrors.{ NineCardsError, PackageNotResolved }
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.catscalaz.TaskInstances.taskMonad
import cards.nine.commons.config.NineCardsConfig.nineCardsConfiguration
import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ CardList, Category, FullCard, Package, PriceFilter }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.processes.GooglePlayApp.GooglePlayApp
import cards.nine.googleplay.processes.getcard.UnknownPackage
import cards.nine.googleplay.processes.{ CardsProcesses, ResolveMany, Wiring }
import cats.{ ~> }
import cats.free.Free
import org.specs2.matcher.{ DisjunctionMatchers, Matcher, Matchers }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaz.{ \/ }
import scalaz.concurrent.Task

class ServicesSpec
  extends Specification
  with Matchers
  with Mockito
  with DisjunctionMatchers {

  import TestData._

  def recommendationIsFreeMatcher(isFree: Boolean): Matcher[FullCard] = { rec: FullCard ⇒
    rec.free must_== isFree
  }

  implicit val actorSystem: ActorSystem = ActorSystem("cards-nine-services-googleplay-tests")
  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] implicit val interpreters: GooglePlayApp ~> Task = new Wiring(nineCardsConfiguration)
  private[this] def run[A](fa: Free[GooglePlayApp, A]): Throwable \/ A =
    fa.foldMap(interpreters).unsafePerformSyncAttempt

  trait BasicScope extends Scope {
    implicit val googlePlayProcesses = mock[CardsProcesses[GooglePlayApp]]
    val services = Services.services[GooglePlayApp]
  }

  object TestData {

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

    val packagesName = List("com.package.one", "com.package.two", "com.package.three", "com.package.four")
    val onePackageName = packagesName.head

    val packages = packagesName map Package
    val onePackage = Package(onePackageName)

    val (validPackagesName, wrongPackagesName) = packagesName.partition(_.length <= 15)

    val validPackages = validPackagesName map Package
    val wrongPackages = wrongPackagesName map Package

    val fullCards = validPackagesName map fullCardFor

    val category = "SHOPPING"

    val limit = 20

    val numPerApp = Some(25)

    val priceFilter = PriceFilter.FREE

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

      val fullCardList = CardList[FullCard](
        missing = wrongPackages,
        pending = Nil,
        cards   = fullCards
      )

      val resolveManyResponse = ResolveMany.Response(wrongPackages, Nil, fullCards)
    }

  }

  "resolveOne" should {
    "return the App object when a valid package name is provided" in new BasicScope {

      googlePlayProcesses.getCard(onePackage, AuthData.marketAuth) returns
        Free.pure(Right(GooglePlayResponses.fullCard))

      val response = services.resolveOne(onePackage, AuthData.marketAuth)
      run(response) must be_\/-[Result[FullCard]].which {
        content ⇒ content must beRight[FullCard](GooglePlayResponses.fullCard)
      }
    }

    "return an error message when a wrong package name is provided" in new BasicScope {

      googlePlayProcesses.getCard(onePackage, AuthData.marketAuth) returns
        Free.pure(Left(GooglePlayResponses.unknwonPackageError))

      val response = services.resolveOne(onePackage, AuthData.marketAuth)

      run(response) must be_\/-[Result[FullCard]].which {
        content ⇒ content must beLeft(PackageNotResolved(onePackageName))
      }
    }
  }

  "resolveMany" should {
    "return the list of apps that are valid and those that are wrong" in new BasicScope {

      googlePlayProcesses.getCards(packages, AuthData.marketAuth) returns
        Free.pure(GooglePlayResponses.resolveManyResponse)

      val response = services.resolveManyDetailed(packages, AuthData.marketAuth)

      run(response) must be_\/-[Result[CardList[FullCard]]].which { response ⇒
        response must beRight[CardList[FullCard]].which { appsInfo ⇒
          appsInfo.missing must containTheSameElementsAs(wrongPackages)
          appsInfo.cards must containTheSameElementsAs(fullCards)
        }
      }
    }
  }

  "recommendByCategory" should {
    "return a list of free recommended apps for the given category" in new BasicScope {

      googlePlayProcesses.recommendationsByCategory(
        Requests.recommendByCategoryRequest,
        AuthData.marketAuth
      ) returns Free.pure(Right(GooglePlayResponses.fullCardList))

      val response = services.recommendByCategory(
        category         = category,
        filter           = priceFilter,
        excludedPackages = wrongPackages,
        limit            = limit,
        auth             = AuthData.marketAuth
      )

      run(response) must be_\/-[Result[CardList[FullCard]]].which { response ⇒
        response must beRight[CardList[FullCard]].which { rec ⇒
          rec.cards must containTheSameElementsAs(fullCards)
        }
      }
    }

    "return a RecommendationsServerError if something went wrong while getting recommendations" in new BasicScope {

      googlePlayProcesses.recommendationsByCategory(
        Requests.recommendByCategoryRequest,
        AuthData.marketAuth
      ) returns Free.pure(Left(GooglePlayResponses.recommendationsInfoError))

      val response = services.recommendByCategory(
        category         = category,
        filter           = priceFilter,
        excludedPackages = wrongPackages,
        limit            = limit,
        auth             = AuthData.marketAuth
      )

      run(response) must be_\/-[Result[CardList[FullCard]]].which { response ⇒
        response must beLeft[NineCardsError]
      }
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

      run(response) must be_\/-[Result[CardList[FullCard]]].which { response ⇒
        response must beRight[CardList[FullCard]].which { rec ⇒
          rec.cards must containTheSameElementsAs(fullCards)
        }
      }
    }
  }
}
