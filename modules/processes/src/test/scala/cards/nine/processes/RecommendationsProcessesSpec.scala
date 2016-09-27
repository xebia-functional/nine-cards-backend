package cards.nine.processes

import cats.free.Free
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.messages.GooglePlayAuthMessages._
import cards.nine.processes.messages.RecommendationsMessages._
import cards.nine.services.free.algebra.GooglePlay.Services
import cards.nine.services.free.domain.GooglePlay.{ AuthParams ⇒ GooglePlayAuthParams, _ }
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

  trait SuccessfulScope extends BasicScope {

    googlePlayServices.recommendByCategory(
      category    = category,
      priceFilter = recommendationFilter,
      auth        = auth.googlePlayAuthParams
    ) returns Free.pure(recommendations)

    googlePlayServices.recommendationsForApps(
      packagesName = packagesName,
      auth         = auth.googlePlayAuthParams
    ) returns Free.pure(recommendations)
  }

}

trait RecommendationsProcessesContext {

  object auth {
    val androidId = "12345"
    val localization = "en_GB"
    val token = "m52_9876"

    val authParams = AuthParams(androidId, Some(localization), token)
    val googlePlayAuthParams = GooglePlayAuthParams(androidId, Some(localization), token)
  }

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  )

  val (excludePackages, recommendedPackages) = packagesName.partition(_.length > 20)

  val title = "Title of the app"

  val free = true

  val icon = "path-to-icon"

  val stars = 4.5

  val downloads = "1000000+"

  val screenshots = List("path-to-screenshot-1", "path-to-screenshot-2")

  val recommendedApps = packagesName map (packageName ⇒
    Recommendation(packageName, title, free, icon, stars, downloads, screenshots))

  val googlePlayRecommendations = recommendedPackages map (packageName ⇒
    GooglePlayRecommendation(packageName, title, free, icon, stars, downloads, screenshots))

  val category = "SOCIAL"

  val limit = 20

  val smallLimit = 1

  val recommendationFilter = "ALL"

  val recommendations = Recommendations(recommendedApps)
}

class RecommendationsProcessesSpec extends RecommendationsProcessesSpecification {

  "getRecommendationsByCategory" should {

    "return a list of recommendations after excluding the given packages" in new SuccessfulScope {
      val response = recommendationsProcesses.getRecommendationsByCategory(
        category,
        recommendationFilter,
        excludePackages,
        limit,
        auth.authParams
      )

      response.foldMap(testInterpreters) must beLike[GetRecommendationsResponse] {
        case r ⇒
          r.items.size must be_<=(limit)
          r.items must_== googlePlayRecommendations
      }
    }

    "return a list of recommendations after excluding the given packages and applying " +
      "the limit" in new SuccessfulScope {
        val response = recommendationsProcesses.getRecommendationsByCategory(
          category,
          recommendationFilter,
          excludePackages,
          smallLimit,
          auth.authParams
        )

        response.foldMap(testInterpreters) must beLike[GetRecommendationsResponse] {
          case r ⇒
            r.items.size must be_==(smallLimit)
            r.items must_== googlePlayRecommendations.take(smallLimit)
        }
      }
  }

  "getRecommendationsForApps" should {

    "return an empty list of recommendations if no packages are given" in new SuccessfulScope {
      val response = recommendationsProcesses.getRecommendationsForApps(
        Nil,
        excludePackages,
        limit,
        auth.authParams
      )

      response.foldMap(testInterpreters) must beLike[GetRecommendationsResponse] {
        case r ⇒
          r.items must beEmpty
          there was noCallsTo(googlePlayServices)
      }
    }

    "return a list of recommendations after excluding the given packages" in new SuccessfulScope {
      val response = recommendationsProcesses.getRecommendationsForApps(
        packagesName,
        excludePackages,
        limit,
        auth.authParams
      )

      response.foldMap(testInterpreters) must beLike[GetRecommendationsResponse] {
        case r ⇒
          r.items.size must be_<=(limit)
          r.items must_== googlePlayRecommendations
      }
    }

    "return a list of recommendations after excluding the given packages and applying " +
      "the limit" in new SuccessfulScope {
        val response = recommendationsProcesses.getRecommendationsForApps(
          packagesName,
          excludePackages,
          smallLimit,
          auth.authParams
        )

        response.foldMap(testInterpreters) must beLike[GetRecommendationsResponse] {
          case r ⇒
            r.items.size must be_==(smallLimit)
            r.items must_== googlePlayRecommendations.take(smallLimit)
        }
      }
  }
}
