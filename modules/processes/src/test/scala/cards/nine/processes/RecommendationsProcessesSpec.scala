package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ CardList, FullCard, Package, PriceFilter }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.processes.NineCardsServices._
import cards.nine.services.free.algebra.GooglePlay.Services
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait RecommendationsProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with RecommendationsProcessesContext
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val googlePlayServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]
    implicit val recommendationsProcesses = new RecommendationsProcesses[NineCardsServices]

  }
}

trait RecommendationsProcessesContext {

  object auth {
    val androidId = "12345"
    val localization = "en_GB"
    val token = "m52_9876"

    val marketAuth = MarketCredentials(AndroidId(androidId), MarketToken(token), Some(Localization(localization)))
  }

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val excludePackages = packagesName.filter(_.value.length > 20)

  val title = "Title of the app"

  val free = true

  val icon = "path-to-icon"

  val stars = 4.5

  val downloads = "1000000+"

  val screenshots = List("path-to-screenshot-1", "path-to-screenshot-2")

  val recommendedApps = packagesName map (packageName ⇒
    FullCard(packageName, title, Nil, downloads, free, icon, screenshots, stars))

  val googlePlayRecommendations = recommendedApps

  val category = "SOCIAL"

  val limit = 20

  val limitPerApp = 100

  val smallLimit = 1

  val recommendationFilter = PriceFilter.ALL

  val recommendations = CardList(Nil, recommendedApps)
}

class RecommendationsProcessesSpec extends RecommendationsProcessesSpecification {

  "getRecommendationsByCategory" should {

    "return a list of recommendations for the given category" in new BasicScope {

      googlePlayServices.recommendByCategory(
        category         = category,
        priceFilter      = recommendationFilter,
        excludesPackages = excludePackages,
        limit            = limit,
        auth             = auth.marketAuth
      ) returns NineCardsService.right(recommendations)

      recommendationsProcesses.getRecommendationsByCategory(
        category,
        recommendationFilter,
        excludePackages,
        limit,
        auth.marketAuth
      ).foldMap(testInterpreters) must beRight[CardList[FullCard]].which { recommendations ⇒
        recommendations.cards must_== googlePlayRecommendations
      }
    }
  }

  "getRecommendationsForApps" should {

    "return an empty list of recommendations if no packages are given" in new BasicScope {

      recommendationsProcesses.getRecommendationsForApps(
        Nil,
        excludePackages,
        limitPerApp,
        limit,
        auth.marketAuth
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
        auth             = auth.marketAuth
      ) returns NineCardsService.right(recommendations)

      recommendationsProcesses.getRecommendationsForApps(
        packagesName,
        excludePackages,
        limitPerApp,
        limit,
        auth.marketAuth
      ).foldMap(testInterpreters) must beRight[CardList[FullCard]].which { recommendations ⇒
        recommendations.cards must_== googlePlayRecommendations
      }
    }
  }
}
