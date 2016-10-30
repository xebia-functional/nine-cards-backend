package cards.nine.googleplay.service.free.interpreter.googleapi

import java.nio.file.{ Files, Paths }

import cards.nine.commons.config.Domain.{ GooglePlayApiConfiguration, GooglePlayApiPaths }
import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.{ MarketCredentials, MarketToken }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cards.nine.googleplay.service.free.algebra.GoogleApi._
import cards.nine.googleplay.service.util.MockServer
import cards.nine.googleplay.util.WithHttp1Client
import cats.data.Xor
import org.mockserver.model.{ Header, HttpRequest, HttpResponse, HttpStatusCode }
import org.specs2.matcher.{ Matchers, TaskMatchers }
import org.specs2.mutable.Specification

class InterpreterSpec extends Specification with Matchers with MockServer with WithHttp1Client {

  import HttpStatusCode._
  import TaskMatchers._
  import TestData.{ fisherPrice, searchCosmos }

  override val mockServerPort = 9995

  val auth = MarketCredentials(AndroidId("androidId"), MarketToken("token"), None)

  val configuration = GooglePlayApiConfiguration(
    protocol = "http",
    host     = "localhost",
    port     = mockServerPort,
    paths    = GooglePlayApiPaths(
      bulkDetails     = "/my/bulkdetails/path",
      details         = "/my/details/path",
      list            = "/path/to/list",
      search          = "/to/searches/path",
      recommendations = "/my/path/to/recommendations"
    )
  )

  private[this] def msHeaders(auth: MarketCredentials): java.util.List[Header] = {
    import scala.collection.JavaConverters._
    def toMS(header: org.http4s.Header): Header =
      new Header(header.name.toString, header.value)
    headers.fullHeaders(auth).toList.map(toMS).asJava
  }

  override def afterAll = {
    super[WithHttp1Client].afterAll
    mockServer.stop
  }

  val interpreter = new Interpreter(configuration)

  def run[A](ops: Ops[A]) = interpreter(ops)(pooledClient)

  def checkTokenErrors[A](request: HttpRequest, ops: Ops[Failure Xor A]) = {

    "give a Too Many Request failure if the API replies with an TooManyRequests status" in {
      val httpResponse = HttpResponse.response.withStatusCode(429)
      mockServer.when(request).respond(httpResponse)
      run(ops) must returnValue(Xor.Left(QuotaExceeded(auth)))
    }

    "return an Unauthorized failure if Google's Api replies with a Unauthorized status" in {
      val httpResponse = HttpResponse.response.withStatusCode(UNAUTHORIZED_401.code)
      mockServer.when(request).respond(httpResponse)
      run(ops) must returnValue(Xor.Left(WrongAuthParams(auth)))
    }

  }

  sequential

  "get Details" should {

    val httpRequest: HttpRequest = HttpRequest.request
      .withMethod("GET")
      .withPath(configuration.paths.details)
      .withQueryStringParameter("doc", fisherPrice.packageName)
      .withHeaders(msHeaders(auth))

    val ops: Ops[Failure Xor FullCard] = GetDetails(fisherPrice.packageObj, auth)

    "return 200 OK and the Full Card for a package if Google's API replies as 200 OK" in {
      val httpResponse: HttpResponse = {
        val protobufFile = getClass.getClassLoader.getResource(fisherPrice.packageName)
        val byteVector = Files.readAllBytes(Paths.get(protobufFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)
      run(ops) must returnValue(Xor.Right(fisherPrice.card))
    }

    "give a PackageNotFound Failure if the Api replies with a NotFound status" in {
      val httpResponse = HttpResponse.response.withStatusCode(NOT_FOUND_404.code)
      mockServer.when(httpRequest).respond(httpResponse)
      run(ops) must returnValue(Xor.Left(PackageNotFound(fisherPrice.packageObj)))
    }

    checkTokenErrors(httpRequest, ops)

  }

  "searchApps" should {

    val httpRequest: HttpRequest = HttpRequest.request
      .withMethod("GET")
      .withPath(configuration.paths.search)
      .withQueryStringParameter("q", searchCosmos.queryWord)
      .withQueryStringParameter("rt", "1")
      .withQueryStringParameter("c", "3")
      .withHeaders(msHeaders(auth))

    val numPacks = 7

    val ops: Ops[Failure Xor List[Package]] = {
      val request = SearchAppsRequest(word = "cosmos", excludedApps = Nil, maxTotal = numPacks)
      SearchApps(request, auth)
    }

    "return 200 OK and a list of packages if Google's API replies as 200 OK" in {
      val httpResponse: HttpResponse = {
        val protobufFile = getClass.getClassLoader.getResource(searchCosmos.fileName)
        val byteVector = Files.readAllBytes(Paths.get(protobufFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)
      run(ops) must returnValue(Xor.Right(searchCosmos.results.take(numPacks)))

    }

    checkTokenErrors(httpRequest, ops)
  }

}

