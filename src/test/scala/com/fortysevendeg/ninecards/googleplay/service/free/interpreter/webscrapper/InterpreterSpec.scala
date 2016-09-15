package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import com.fortysevendeg.ninecards.googleplay.domain.Package
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._
import com.fortysevendeg.ninecards.googleplay.service.util.MockServer
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
import org.mockserver.model.HttpRequest._
import org.mockserver.model.HttpResponse._
import org.mockserver.model.HttpStatusCode
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification


class InterpreterSpec extends Specification with Matchers with MockServer with WithHttp1Client {

  import TestData._

  override val mockServerPort = 9998

  private[this] def existsRequest(pack: Package) = request
    .withMethod("HEAD")
    .withPath(detailsPath)
    .withQueryStringParameter( "hl", "en_US")
    .withQueryStringParameter( "id", goodPackage.value)

  mockServer
    when( existsRequest(goodPackage) )
    respond response.withStatusCode(HttpStatusCode.OK_200.code)

  mockServer
    when existsRequest(badPackage)
    respond response.withStatusCode(HttpStatusCode.NOT_FOUND_404.code)

  implicit val configuration = Configuration(
    protocol    = "http",
    host        = "localhost",
    port        = mockServerPort,
    detailsPath = detailsPath
  )

  override def afterAll = {
    super[WithHttp1Client].afterAll
    super[MockServer].afterAll
  }


  val interpreter = new Interpreter(configuration, pooledClient)

  sequential

  "existsApp" should {

    "return true if the server gives a 200 OK Status" in {
      interpreter( ExistsApp(goodPackage) ).run should beTrue
    }

    "return false if the server gives a 404 NotFound Status" in {
      interpreter( ExistsApp(goodPackage) ).run should beFalse
    }
  }

}


object TestData {

  val detailsPath = "/store/apps/details"
  val goodPackage = Package( "goodPack" )
  val badPackage = Package( "badPack" )

}
