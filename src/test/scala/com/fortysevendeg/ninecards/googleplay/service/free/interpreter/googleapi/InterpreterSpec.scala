package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.apigoogle._
import com.fortysevendeg.ninecards.googleplay.service.util.MockServer
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
import java.nio.file.{Files, Paths}
import org.mockserver.model.{HttpRequest, HttpResponse, HttpStatusCode, Header}
import org.specs2.matcher.{Matchers, TaskMatchers}
import org.specs2.mutable.Specification

class InterpreterSpec extends Specification with Matchers with MockServer with WithHttp1Client {

  import HttpStatusCode._
  import TaskMatchers._
  import TestData.fisherPrice

  override val mockServerPort = 9997

  val auth = GoogleAuthParams( AndroidId("androidId"), Token("token"), None)

  val configuration = Configuration(
    protocol    = "http",
    host        = "localhost",
    port        = mockServerPort,
    detailsPath = "/my/details/path",
    listPath    = "/path/to/list",
    recommendationsPath = "/my/path/to/recommendations"
  )

  private[this] def msHeaders(auth: GoogleAuthParams): java.util.List[Header] = {
    import scala.collection.JavaConverters._
    def toMS(header: org.http4s.Header): Header = 
      new Header( header.name.toString, header.value)
    headers.fullHeaders(auth).toList.map(toMS).asJava
  }

  override def afterAll = {
    super[WithHttp1Client].afterAll
    mockServer.stop
  }

  val interpreter = new Interpreter(configuration)

  def run[A](ops: Ops[A]) = interpreter(ops)(pooledClient)

  sequential

  "get Details" should {

    val httpRequest: HttpRequest = HttpRequest.request
      .withMethod("GET")
      .withPath(configuration.detailsPath)
      .withQueryStringParameter( "doc", fisherPrice.packageName)
      .withHeaders( msHeaders(auth) )

    "return 200 OK and the Full Card for a package if Google's API replies as 200 OK" in {
      val httpResponse: HttpResponse = {
        val protobufFile = getClass.getClassLoader.getResource(fisherPrice.packageName)
        val byteVector = Files.readAllBytes(Paths.get(protobufFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)

      val actual = run( GetDetails(fisherPrice.packageObj, auth) )
      println(actual.run)
      actual must returnValue( Xor.Right( fisherPrice.card) ) 
    }

    "return an Unauthorized failure if Google's Api replies with a Unauthorized status" in {
      val httpResponse: HttpResponse =
        HttpResponse.response.withStatusCode(UNAUTHORIZED_401.code)
      mockServer.when(httpRequest).respond(httpResponse)

      val actual = run( GetDetails(fisherPrice.packageObj, auth) )
      actual must returnValue( Xor.Left( WrongAuthParans( auth) ) )
    }

    "give a PackageNotFound Failure if the Api replies with a NotFound status" in {
      val httpResponse: HttpResponse =
        HttpResponse.response.withStatusCode(NOT_FOUND_404.code)
      mockServer.when(httpRequest).respond(httpResponse)

      val actual = run( GetDetails(fisherPrice.packageObj, auth) )
      actual must returnValue( Xor.Left( PackageNotFound( fisherPrice.packageObj ) ) )
    }

    "give a Too Many Request failure if the API replies with an TooManyRequests status" in {
      val httpResponse: HttpResponse =
        HttpResponse.response.withStatusCode(429)
      mockServer.when(httpRequest).respond(httpResponse)

      val actual = run( GetDetails(fisherPrice.packageObj, auth) )
      actual must returnValue( Xor.Left( QuotaExceeded(auth)) )
    }

  }

}

object  TestData {

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    lazy val protobufFile = getClass.getClassLoader.getResource(packageName)
    lazy val htmlFile     = getClass.getClassLoader.getResource(packageName + ".html")

    val card = FullCard(
      packageName = packageName,
      title = "Shapes & Colors Music Show",
      free = true,
      icon = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H",
      stars = 4.070538520812988,
      downloads = "1,000,000+",
      screenshots = List(
        "http://lh4.ggpht.com/fi-LxRsm8E5-940Zc5exQQyb4WWt1Q9D4oQFfEMP9oX0sWgV2MmIVAKwjtMN7ns5k7M",
        "http://lh3.ggpht.com/3ojygv7ZArhODcEq_JTaYx8ap4WwrgU6qYzspYyuEH24byhtqsgSaS0W9YN6A8ySSXA",
        "http://lh4.ggpht.com/974sdpZY4MiXIDn4Yyutylbh7cecJ7nKhHUz3LA3fAR3HdPwyM3yFUOdmcSlCwWjJiYc"),
      categories = List("EDUCATION")
    )
  }


}