package cards.nine.processes

import cats.free.Free
import cards.nine.processes.NineCardsServices._
import cards.nine.processes.messages.ApplicationMessages._
import cards.nine.processes.messages.GooglePlayAuthMessages._
import cards.nine.services.free.algebra.GooglePlay.Services
import cards.nine.services.free.domain.GooglePlay.{ AppInfo, AppsInfo, AuthParams ⇒ GooglePlayAuthParams }
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait ApplicationProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with ApplicationProcessesContext
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val googlePlayServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]
    implicit val applicationProcesses = new ApplicationProcesses[NineCardsServices]

  }

  trait SuccessfulScope extends BasicScope {

    googlePlayServices.resolveMany(packagesName, googlePlayAuthParams) returns Free.pure(appsInfo)

  }

}

trait ApplicationProcessesContext {

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  )

  val title = "Title of the app"

  val free = true

  val icon = "path-to-icon"

  val stars = 4.5

  val downloads = "1000000+"

  val category = "SOCIAL"

  val (missing, found) = packagesName.partition(_.length > 6)

  val apps = found map (packageName ⇒ AppInfo(packageName, title, free, icon, stars, downloads, List(category)))
  val appsGooglePlayInfo = found map (packageName ⇒ AppGooglePlayInfo(packageName, title, free, icon, stars, downloads, List(category)))

  val androidId = "12345"
  val localization = "en_GB"
  val token = "m52_9876"

  val authParams = AuthParams(androidId, Some(localization), token)
  val googlePlayAuthParams = GooglePlayAuthParams(androidId, Some(localization), token)

  val emptyGetAppsInfoResponse = GetAppsInfoResponse(
    errors = Nil,
    items  = Nil
  )

  val appsInfo = AppsInfo(missing, apps)
}

class ApplicationProcessesSpec extends ApplicationProcessesSpecification {

  "getAppsInfo" should {
    "return an empty response without calling the Google Play service if an empty list of" +
      "packages name is passed" in new BasicScope {
        val response = applicationProcesses.getAppsInfo(Nil, authParams)

        response.foldMap(testInterpreters) must_== emptyGetAppsInfoResponse
      }

    "return a valid response if a non empty list of packages name is passed" in new SuccessfulScope {
      val response = applicationProcesses.getAppsInfo(packagesName, authParams)

      response.foldMap(testInterpreters) must beLike[GetAppsInfoResponse] {
        case r ⇒
          r.errors must_== missing
          r.items must_== appsGooglePlayInfo
      }
    }
  }
}
