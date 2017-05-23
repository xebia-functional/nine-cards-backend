/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.service.free.interpreter.googleapi

import java.nio.file.{ Files, Paths }

import cards.nine.commons.config.Domain.{ GooglePlayApiConfiguration, GooglePlayApiPaths }
import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.{ MarketCredentials, MarketToken }
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cards.nine.googleplay.service.util.MockServer
import cards.nine.googleplay.util.WithHttp1Client
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
    protocol            = "http",
    host                = "localhost",
    port                = mockServerPort,
    detailsBatchSize    = 5,
    maxTotalConnections = 15,
    paths               = GooglePlayApiPaths(
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

  val cc = new Interpreter(configuration)

  def run[A](ops: WithHttpClient[A]) = ops(pooledClient)

  def checkTokenErrors[A](request: HttpRequest, ops: WithHttpClient[Failure Either A]) = {

    "give a Too Many Request failure if the API replies with an TooManyRequests status" in {
      val httpResponse = HttpResponse.response.withStatusCode(429)
      mockServer.when(request).respond(httpResponse)
      run(ops) must returnValue(Left(QuotaExceeded(auth)))
    }

    "return an Unauthorized failure if Google's Api replies with a Unauthorized status" in {
      val httpResponse = HttpResponse.response.withStatusCode(UNAUTHORIZED_401.code)
      mockServer.when(request).respond(httpResponse)
      run(ops) must returnValue(Left(WrongAuthParams(auth)))
    }

  }

  sequential

  "get Details" should {

    val httpRequest: HttpRequest = HttpRequest.request
      .withMethod("GET")
      .withPath(configuration.paths.details)
      .withQueryStringParameter("doc", fisherPrice.packageName)
      .withHeaders(msHeaders(auth))

    val ops: WithHttpClient[Failure Either FullCard] = cc.getDetails(fisherPrice.packageObj, auth)

    "return 200 OK and the Full Card for a package if Google's API replies as 200 OK" in {
      val httpResponse: HttpResponse = {
        val protobufFile = getClass.getClassLoader.getResource(fisherPrice.packageName)
        val byteVector = Files.readAllBytes(Paths.get(protobufFile.getFile))
        HttpResponse.response
          .withStatusCode(OK_200.code)
          .withBody(byteVector)
      }
      mockServer.when(httpRequest).respond(httpResponse)
      run(ops) must returnValue(Right(fisherPrice.card))
    }

    "give a PackageNotFound Failure if the Api replies with a NotFound status" in {
      val httpResponse = HttpResponse.response.withStatusCode(NOT_FOUND_404.code)
      mockServer.when(httpRequest).respond(httpResponse)
      run(ops) must returnValue(Left(PackageNotFound(fisherPrice.packageObj)))
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

    val ops: WithHttpClient[Failure Either List[Package]] = {
      val request = SearchAppsRequest(word = "cosmos", excludedApps = Nil, maxTotal = numPacks)
      cc.searchApps(request, auth)
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
      run(ops) must returnValue(Right(searchCosmos.results.take(numPacks)))

    }

    checkTokenErrors(httpRequest, ops)
  }

}

