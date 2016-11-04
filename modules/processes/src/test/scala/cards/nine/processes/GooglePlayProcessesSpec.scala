package cards.nine.processes

import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ FullCard, FullCardList, Package }
import cards.nine.domain.market.{ MarketCredentials, MarketToken }
import cards.nine.processes.NineCardsServices.NineCardsServices
import cards.nine.services.free.algebra.GooglePlay.Services
import cats.free.Free
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait GooglePlayProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with GooglePlayProcessesContext
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val googlePlayServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]
    implicit val applicationProcesses = new ApplicationProcesses[NineCardsServices]

    googlePlayServices.resolveManyDetailed(Nil, marketAuth) returns
      Free.pure(FullCardList(Nil, Nil))

    googlePlayServices.resolveManyDetailed(packageNames, marketAuth) returns
      Free.pure(FullCardList(missing, apps))
  }
}

trait GooglePlayProcessesContext {

  val packageNames = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val (missing, foundPackageNames) = packageNames.partition(_.value.length < 20)

  val apps = foundPackageNames map { packageName ⇒
    FullCard(
      packageName = packageName,
      title       = "Title of app",
      free        = true,
      icon        = "Icon of app",
      stars       = 5.0,
      downloads   = "Downloads of app",
      screenshots = Nil,
      categories  = List("Country")
    )
  }

  val marketAuth = MarketCredentials(AndroidId("androidId"), MarketToken("token"), None)
}

class GooglePlayProcessesSpec extends GooglePlayProcessesSpecification {

  "categorizeApps" should {
    "return empty items and errors lists if an empty list of apps is provided" in new BasicScope {
      val response = applicationProcesses.getAppsInfo(Nil, marketAuth).foldMap(testInterpreters)

      response.missing should beEmpty
      response.cards should beEmpty
    }

    "return items and errors lists for a non empty list of apps" in new BasicScope {
      val response = applicationProcesses.getAppsInfo(packageNames, marketAuth).foldMap(testInterpreters)

      response.missing shouldEqual missing
      forall(response.cards) { item ⇒
        apps.exists(app ⇒
          app.packageName == item.packageName &&
            app.categories.nonEmpty &&
            app.categories == item.categories) should beTrue
      }
    }
  }
}
