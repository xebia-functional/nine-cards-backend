package cards.nine.googleplay.processes

import cats.~>
import cats.data.Xor
import cards.nine.googleplay.config.TestConfig._
import cards.nine.googleplay.domain._
import cards.nine.googleplay.service.free.interpreter.TestData._
import cards.nine.googleplay.service.free.interpreter.webscrapper.Http4sGooglePlayWebScraper
import cards.nine.googleplay.processes.GooglePlay._
import cards.nine.googleplay.util.WithHttp1Client
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification
import scala.concurrent.duration._
import scalaz.concurrent.Task

class TaskInterpreterIntegration extends Specification with TaskMatchers with WithHttp1Client {

  sequential

  val webClient = new Http4sGooglePlayWebScraper(webEndpoint, pooledClient)

  val wiring = new Wiring()

  val interpreter: (Ops ~> Task) = wiring.interpreter

  override def afterAll = wiring.shutdown

  def categoryOption(item: Item): Option[String] =
    item.docV2.details.appDetails.appCategory.headOption

  "Making requests to the Google Play store" should {

    def splitResults(res: PackageDetails): (List[String], List[String]) = (
      res.errors.sorted,
      res.items.flatMap(categoryOption).sorted
    )

    "result in a correctly parsed response for a single package" in {
      val retrievedCategory: Task[Option[String]] = interpreter
        .apply(Resolve(authParams, fisherPrice.packageObj))
        .map(optItem ⇒ optItem.flatMap(categoryOption))
      retrievedCategory must returnValue(Some("EDUCATION"))
    }

    "result in a correctly parsed response for multiple packages" in {
      val successfulCategories = List(
        (fisherPrice.packageName, "EDUCATION"),
        ("com.google.android.googlequicksearchbox", "TOOLS")
      )

      val invalidPackages = List(nonexisting.packageName, "com.another.invalid.package")
      val packages: List[String] = successfulCategories.map(_._1) ++ invalidPackages

      val result = interpreter
        .apply(ResolveMany(authParams, PackageList(packages)))
        .map(splitResults)
      result must returnValue((invalidPackages.sorted, successfulCategories.map(_._2).sorted))
    }
  }

  "Making requests when the Google Play API is not successful" should {
    "fail over to the web scraping approach" in {

      val interpreter = {
        val badApiRequest: AppRequest ⇒ Task[Xor[String, Item]] =
          (_ ⇒ Task.fail(new RuntimeException("Failed request")))
        val itemService: AppRequest ⇒ Task[Xor[String, Item]] =
          new XorTaskOrComposer(badApiRequest, webClient.getItem)
        val appCardService: AppRequest ⇒ Task[Xor[InfoError, FullCard]] =
          (_ ⇒ Task.fail(new RuntimeException("Should not ask for App Card")))
        val recommendByCategory: RecommendationsByCategory ⇒ Task[Xor[InfoError, FullCardList]] =
          (_ ⇒ Task.fail(new RuntimeException("Should not ask for recommendations")))
        val recommendByAppList: RecommendationsByAppList ⇒ Task[FullCardList] =
          (_ ⇒ Task.fail(new RuntimeException("Should not ask for recommendations")))
        new TaskInterpreter(itemService, appCardService, recommendByCategory, recommendByAppList)
      }

      val retrievedCategory: Task[Option[String]] = interpreter
        .apply(Resolve(authParams, fisherPrice.packageObj))
        .map(_.flatMap(categoryOption))

      retrievedCategory.unsafePerformSyncFor(10.seconds) must_=== Some("EDUCATION")
    }
  }
}
