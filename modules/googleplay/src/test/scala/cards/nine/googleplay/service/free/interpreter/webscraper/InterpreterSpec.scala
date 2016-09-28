package cards.nine.googleplay.service.free.interpreter.webscrapper

import cats.data.Xor
import cards.nine.googleplay.domain.{Package, FullCard}
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.webscrapper._
import cards.nine.googleplay.service.util.MockServer
import cards.nine.googleplay.util.WithHttp1Client
import java.nio.file.{Files, Paths}
import org.mockserver.model.{HttpRequest, HttpResponse}
import org.mockserver.model.HttpStatusCode._
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification


class InterpreterSpec extends Specification with Matchers with MockServer with WithHttp1Client {

  import TestData._

  implicit val configuration = Configuration(
    protocol    = "http",
    host        = "localhost",
    port        = mockServerPort,
    detailsPath = detailsPath
  )

  override def afterAll = {
    super[WithHttp1Client].afterAll
    mockServer.stop
  }

  val interpreter = new Interpreter(configuration)

  def run[A](ops: Ops[A]) = interpreter(ops)(pooledClient)


  sequential

  "existsApp" should {

    val httpRequest = HttpRequest.request
      .withMethod("HEAD")
      .withPath(detailsPath)
      .withQueryStringParameter( "hl", "en_US")
      .withQueryStringParameter( "id", thePackage.value)

    def runOperation = run( ExistsApp(thePackage)).unsafePerformSync

    "return true if the server gives a 200 OK Status" in {
      val httpResponse = HttpResponse.response.withStatusCode(OK_200.code)
      mockServer.when( httpRequest).respond(httpResponse)
      runOperation should beTrue
    }

    "return false if the server gives a 404 NotFound Status" in {
      val httpResponse = HttpResponse.response.withStatusCode(NOT_FOUND_404.code) 
      mockServer.when( httpRequest).respond(httpResponse)
      runOperation should beFalse
    }
  }

  "getDetails" should {

    val httpRequest = HttpRequest.request
      .withMethod("GET")
      .withPath(detailsPath)
      .withQueryStringParameter( "hl", "en_US")
      .withQueryStringParameter( "id", fisherPrice.packageName)

    def runOperation(pack: Package) = interpreter( GetDetails(pack) )(pooledClient).unsafePerformSync

    "return the card if the server gives a 200 OK Status" in {
      val httpResponse = {
        val byteVector = Files.readAllBytes(Paths.get(fisherPrice.htmlFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)
      runOperation(fisherPrice.packageObj)  must_=== Xor.Right(fisherPrice.card)
    }

    "return a PackageNotFound(_) failure if server gives 404 NotFound status" in {
      val httpResponse = HttpResponse.response.withStatusCode( NOT_FOUND_404.code)
      mockServer.when(httpRequest).respond(httpResponse)
      runOperation(fisherPrice.packageObj) must_=== Xor.Left( PackageNotFound( fisherPrice.packageObj) )
    }

    "For the SkyMap play store web app" in {
      val httpRequest = HttpRequest.request
        .withMethod("GET")
        .withPath(detailsPath)
        .withQueryStringParameter( "hl", "en_US")
        .withQueryStringParameter( "id", skymap.packageName)

      val httpResponse = {
        val byteVector = Files.readAllBytes(Paths.get(skymap.htmlFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)

      val result = runOperation(skymap.packageObj)
      result must_=== Xor.Right(skymap.card)
    }

  }

}


object TestData {

  val detailsPath = "/store/apps/details"
  val thePackage = Package("any.package.name")

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageName,
      title = "Shapes & Colors Music Show",
      free = true,
      icon = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H",
      stars = 4.069400310516357,
      downloads = "1.000.000 - 5.000.000",
      screenshots = List(
        "http://lh4.ggpht.com/fi-LxRsm8E5-940Zc5exQQyb4WWt1Q9D4oQFfEMP9oX0sWgV2MmIVAKwjtMN7ns5k7M",
        "http://lh3.ggpht.com/3ojygv7ZArhODcEq_JTaYx8ap4WwrgU6qYzspYyuEH24byhtqsgSaS0W9YN6A8ySSXA",
        "http://lh4.ggpht.com/974sdpZY4MiXIDn4Yyutylbh7cecJ7nKhHUz3LA3fAR3HdPwyM3yFUOdmcSlCwWjJiYc"
      ),
      categories = List("EDUCATION", "FAMILY_EDUCATION")
    )

    val protobufFile = getClass.getClassLoader.getResource(fisherPrice.packageName)
    val htmlFile = getClass.getClassLoader.getResource(packageName + ".html")
  }

  object skymap {
    val packageName = "com.google.android.stardroid"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageName,
      title = "Sky Map",
      free = true,
      icon = "http://lh4.ggpht.com/4VGiZutofCjs_wEC3BOuFPXysyF-ClYDTa40F3qK-GhKcISkWFFpRiBFmD8HPDTrElQ",
      stars = 4.491560935974121,
      downloads = "50.000.000 - 100.000.000",
      screenshots = List(
        "http://lh4.ggpht.com/Ag5QSMMtWqxi3UTFW7y239mT0khsMvBNPVqkdwuadr6Ar2vMV9vZFyzoHvGNOTNYWA0",
        "http://lh6.ggpht.com/veDf0tA3YTKBbavlTbITigF04iZ3lEKcNrZKwZJktCL8fn-cGLCW9Ifk-g8ICduZgw",
        "http://lh3.ggpht.com/_9FKlrlbRz4phkAIDqtsLAnl5Yi3WmLhh6Tv8KCuy2x5FWiMBlm-qfTqXvgET2F5x7E",
        "http://lh4.ggpht.com/PagtRs82S_uWgzGApzfQBg5iJmspg6H1ByPPq2qtctMufz2Nar4hxaj4aCh7-N4ahKk",
        "http://lh5.ggpht.com/ChobWjgx7U1l3tFoQr09IXlIXr-i7DtFy7Pmek1evqXOFOaZPhJ3SnW8EEvT_WlK-HA",
        "http://lh4.ggpht.com/yfX-_XD-YzNz1sKbA7vUgN_yEBXHpCzf8rHSxKDlUNbF-EID8hDfHvtmlYpczlF4UoM",
        "http://lh3.googleusercontent.com/E01joGlVkodgK91jqwi6oXlH9ChsE8Z93nihL8g5N1kXOYyE-CFRhZ8gyTJRxGM6rFEI",
        "http://lh3.googleusercontent.com/S0mWOIoo9OnxNlP7_sgQuhp4m-tq-sA-4zxgJ7uQPmPpiI3rmZIqqkMU0ml-DWGidUA"
      ),
      categories = List("BOOKS_AND_REFERENCE")
    )

    val htmlFile = getClass.getClassLoader.getResource(packageName + ".html")
  }

}
