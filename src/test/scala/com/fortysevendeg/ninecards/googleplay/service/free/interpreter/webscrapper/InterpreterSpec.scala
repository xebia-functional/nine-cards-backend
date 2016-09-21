package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import com.fortysevendeg.ninecards.googleplay.domain.Package
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._
import com.fortysevendeg.ninecards.googleplay.service.util.MockServer
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
import org.mockserver.model.{HttpRequest, HttpResponse, HttpStatusCode}
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

  val interpreter = new Interpreter(configuration, pooledClient)

  sequential

  "existsApp" should {

    val httpRequest = HttpRequest.request
      .withMethod("HEAD")
      .withPath(detailsPath)
      .withQueryStringParameter( "hl", "en_US")
      .withQueryStringParameter( "id", thePackage.value)

    def runOperation = interpreter( ExistsApp(thePackage) ).run

    "return true if the server gives a 200 OK Status" in {
      val httpResponse = HttpResponse.response.withStatusCode(HttpStatusCode.OK_200.code)
      mockServer.when( httpRequest).respond(httpResponse)
      runOperation should beTrue
    }

    "return false if the server gives a 404 NotFound Status" in {
      val httpResponse = HttpResponse.response.withStatusCode(HttpStatusCode.NOT_FOUND_404.code) 
      mockServer.when( httpRequest).respond(httpResponse)
      runOperation should beFalse
    }
  }

}


object TestData {

  val detailsPath = "/store/apps/details"
  val thePackage = Package("any.package.name")

}
