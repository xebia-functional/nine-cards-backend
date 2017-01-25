package cards.nine.processes.applications

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.application.{ CardList, FullCard }
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.TestInterpreters
import cards.nine.services.free.algebra.GooglePlay.Services
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait ApplicationProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val googlePlayServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]
    implicit val applicationProcesses = new ApplicationProcesses[NineCardsServices]

  }
}

class ApplicationProcessesSpec extends ApplicationProcessesSpecification {

  import TestData.{ category, marketAuth, packagesName }

  "getAppsInfo" should {
    import TestData._

    "return an empty response without calling the Google Play service if an empty list of" +
      "packages name is passed" in new BasicScope {

        applicationProcesses.getAppsInfo(Nil, marketAuth).foldMap(testInterpreters) must beRight(emptyGetAppsInfoResponse)
      }

    "return a valid response if a non empty list of packages name is passed" in new BasicScope {

      googlePlayServices.resolveManyDetailed(packagesName, marketAuth) returns
        NineCardsService.right(appsInfo)

      applicationProcesses
        .getAppsInfo(packagesName, marketAuth)
        .foldMap(testInterpreters) must beRight[CardList[FullCard]].which { response ⇒
          response.missing must_== missing
          response.cards must_== apps
        }
    }
  }

  "categorizeApps" should {
    import GooglePlayTestData._

    "return empty items and errors lists if an empty list of apps is provided" in new BasicScope {

      googlePlayServices.resolveManyDetailed(Nil, marketAuth) returns
        NineCardsService.right(CardList(Nil, Nil, Nil))

      applicationProcesses
        .getAppsInfo(Nil, marketAuth)
        .foldMap(testInterpreters) must beRight[CardList[FullCard]].which { response ⇒

          response.missing must beEmpty
          response.cards must beEmpty
        }
    }

    "return items and errors lists for a non empty list of apps" in new BasicScope {

      googlePlayServices.resolveManyDetailed(packagesName, marketAuth) returns
        NineCardsService.right(CardList(missing, Nil, apps))

      applicationProcesses
        .getAppsInfo(packagesName, marketAuth)
        .foldMap(testInterpreters) must beRight[CardList[FullCard]].which { response ⇒

          response.missing must containTheSameElementsAs(missing)
          response.cards must containTheSameElementsAs(apps)
        }
    }
  }

  "getRecommendationsByCategory" should {

    import RecommendationsTestData._
    "return a list of recommendations for the given category" in new BasicScope {

      googlePlayServices.recommendByCategory(
        category         = category,
        priceFilter      = recommendationFilter,
        excludesPackages = excludePackages,
        limit            = limit,
        auth             = marketAuth
      ) returns NineCardsService.right(recommendations)

      applicationProcesses.getRecommendationsByCategory(
        category,
        recommendationFilter,
        excludePackages,
        limit,
        marketAuth
      ).foldMap(testInterpreters) must beRight[CardList[FullCard]].which { recommendations ⇒
        recommendations.cards must_== recommendedApps
      }
    }
  }

  "getRecommendationsForApps" should {

    import RecommendationsTestData._
    "return an empty list of recommendations if no packages are given" in new BasicScope {

      applicationProcesses.getRecommendationsForApps(
        Nil,
        excludePackages,
        limitPerApp,
        limit,
        marketAuth
      ).foldMap(testInterpreters) must beRight[CardList[FullCard]].which { recommendations ⇒
        recommendations.cards must beEmpty
        there was noCallsTo(googlePlayServices)
      }
    }

    "return a list of recommendations for the given packages" in new BasicScope {

      googlePlayServices.recommendationsForApps(
        packagesName     = packagesName,
        excludesPackages = excludePackages,
        limitPerApp      = limitPerApp,
        limit            = limit,
        auth             = marketAuth
      ) returns NineCardsService.right(recommendations)

      applicationProcesses.getRecommendationsForApps(
        packagesName,
        excludePackages,
        limitPerApp,
        limit,
        marketAuth
      ).foldMap(testInterpreters) must beRight[CardList[FullCard]].which { recommendations ⇒
        recommendations.cards must_== recommendedApps
      }
    }
  }

}
