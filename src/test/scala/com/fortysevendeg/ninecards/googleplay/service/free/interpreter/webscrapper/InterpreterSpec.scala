package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain.{Package, FullCard}
import com.fortysevendeg.ninecards.googleplay.domain.webscrapper._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._
import com.fortysevendeg.ninecards.googleplay.service.util.MockServer
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
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

    def runOperation = run( ExistsApp(thePackage)).run

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

    def runOperation = interpreter( GetDetails(fisherPrice.packageObj) )(pooledClient).run

    "return the card if the server gives a 200 OK Status" in {
      val httpResponse = {
        val byteVector = Files.readAllBytes(Paths.get(fisherPrice.htmlFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)
      runOperation must_=== Xor.Right(fisherPrice.card)
    }

    "return a PackageNotFound(_) failure if server gives 404 NotFound status" in {
      val httpResponse = HttpResponse.response.withStatusCode( NOT_FOUND_404.code)
      mockServer.when(httpRequest).respond(httpResponse)
      runOperation must_=== Xor.Left( PackageNotFound( fisherPrice.packageObj) )
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
    val htmlFile     = getClass.getClassLoader.getResource(packageName + ".html")
  }

}
