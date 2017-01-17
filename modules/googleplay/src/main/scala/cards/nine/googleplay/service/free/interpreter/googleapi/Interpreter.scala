package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.domain.application.{ FullCard, Package, PriceFilter, BasicCard }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain._
import cards.nine.googleplay.domain.apigoogle._
import cards.nine.googleplay.proto.GooglePlay.{ BulkDetailsRequest, DocV2, ResponseWrapper }
import cards.nine.googleplay.service.free.algebra.GoogleApi._
import cats.instances.either._
import cats.syntax.either._
import cats.~>
import cats.instances.list._
import cats.syntax.monadCombine._
import cats.syntax.traverse._
import java.net.URLEncoder

import cards.nine.commons.config.Domain.GooglePlayApiConfiguration
import org.http4s.Http4s._
import org.http4s._
import org.http4s.client.{ Client, UnexpectedStatus }

import scala.collection.JavaConversions._
import collection.JavaConverters._
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Interpreter(config: GooglePlayApiConfiguration) extends (Ops ~> WithHttpClient) {

  def apply[A](ops: Ops[A]): WithHttpClient[A] = ops match {
    case GetBulkDetails(packs, auth) ⇒ new BulkDetailsWithClient(packs, auth)
    case GetDetails(pack, auth) ⇒ new DetailsWithClient(pack, auth)
    case RecommendationsByApps(request, auth) ⇒ new RecommendationsByAppsWithClient(request, auth)
    case RecommendationsByCategory(request, auth) ⇒ new RecommendationsByCategoryWithClient(request, auth)
    case SearchApps(request, auth) ⇒ new SearchAppsWithClient(request, auth)
  }

  private[this] val baseUri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  class BulkDetailsWithClient(packagesName: List[Package], auth: MarketCredentials)
    extends (Client ⇒ Task[Failure Either List[BasicCard]]) {

    val builder = BulkDetailsRequest.newBuilder()
    builder.addAllDocid(packagesName.map(_.value).asJava)

    val httpRequest =
      new Request(
        method  = Method.POST,
        uri     = baseUri.withPath(config.paths.bulkDetails),
        headers = headers.fullHeaders(auth, Option("application/x-protobuf"))
      ).withBody(builder.build().toByteArray)

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests ⇒ QuotaExceeded(auth)
      case _ ⇒ GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Either List[BasicCard]] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        Either.right {
          ResponseWrapper
            .parseFrom(bv.toArray)
            .getPayload.getBulkDetailsResponse
            .getEntryList
            .toList
            .map(entry ⇒ Converters.toBasicCard(entry.getDoc))
            .filterNot(_.packageName.value.isEmpty)
        }
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(handleUnexpected(e))
      }
  }

  class DetailsWithClient(packageName: Package, auth: MarketCredentials)
    extends (Client ⇒ Task[Failure Either FullCard]) {

    val httpRequest: Request =
      new Request(
        method  = Method.GET,
        uri     = baseUri
          .withPath(config.paths.details)
          .withQueryParam("doc", packageName.value),
        headers = headers.fullHeaders(auth)
      )

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.NotFound ⇒ PackageNotFound(packageName)
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests ⇒ QuotaExceeded(auth)
      case _ ⇒ GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Either FullCard] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        val docV2: DocV2 = ResponseWrapper.parseFrom(bv.toArray).getPayload.getDetailsResponse.getDocV2
        val fullCard = Converters.toFullCard(docV2)
        Either.right(fullCard)
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(handleUnexpected(e))
      }
  }

  class RecommendationsByAppsWithClient(request: RecommendByAppsRequest, auth: MarketCredentials)
    extends (Client ⇒ Task[List[Package]]) {

    def recommendationsByApp(client: Client)(pack: Package): Task[InfoError Either List[Package]] = {

      val httpRequest: Request = new Request(
        method  = Method.GET,
        uri     = baseUri
          .withPath(config.paths.recommendations)
          .withQueryParam("c", 3)
          .withQueryParam("rt", "1")
          .withQueryParam("doc", pack.value),
        headers = headers.fullHeaders(auth)
      )

      def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
        case Status.NotFound ⇒ PackageNotFound(pack)
        case Status.Unauthorized ⇒ WrongAuthParams(auth)
        case Status.TooManyRequests ⇒ QuotaExceeded(auth)
        case _ ⇒ GoogleApiServerError
      }

      client.expect[ByteVector](httpRequest).map { bv ⇒
        Either.right(ResponseWrapper.parseFrom(bv.toArray).getPayload.getListResponse)
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(handleUnexpected(e))
      }.map {
        _.bimap(
          _ ⇒ InfoError(s"Recommendations for package ${pack.value}"),
          list ⇒ {
            val packs = Converters.listResponseToPackages(list)
            request.numPerApp.fold(packs)(packs.take)
          }
        )
      }
    }

    def joinLists(xors: List[InfoError Either List[Package]]): List[Package] = {
      val (_, packages) = xors.separate
      packages.flatten.distinct.diff(request.excludedApps).take(request.maxTotal)
    }

    def apply(client: Client): Task[List[Package]] = {
      request.searchByApps.traverse(recommendationsByApp(client)).map(joinLists)
    }
  }

  class RecommendationsByCategoryWithClient(request: RecommendByCategoryRequest, auth: MarketCredentials)
    extends (Client ⇒ Task[InfoError Either List[Package]]) {

    val infoError = InfoError(s"Recommendations for category ${request.category} that are ${request.priceFilter}")

    val subCategory: String = request.priceFilter match {
      case PriceFilter.FREE ⇒ "apps_topselling_free"
      case PriceFilter.PAID ⇒ "apps_topselling_paid"
      case PriceFilter.ALL ⇒ "apps_topgrossing"
    }

    val httpRequest: Request = new Request(
      method  = Method.GET,
      uri     = baseUri
        .withPath(config.paths.list)
        .withQueryParam("c", "3")
        .withQueryParam("cat", request.category.entryName)
        .withQueryParam("ctr", subCategory),
      headers = headers.fullHeaders(auth)
    )

    def apply(client: Client): Task[InfoError Either List[Package]] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        Either.right(ResponseWrapper.parseFrom(bv.toArray).getPayload.getListResponse)
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(GoogleApiServerError)
      }.map {
        case Left(_) ⇒ Either.left(infoError)
        case Right(listResponse) ⇒ Either.right(Converters.listResponseToPackages(listResponse))
      }
  }

  class SearchAppsWithClient(request: SearchAppsRequest, auth: MarketCredentials)
    extends (Client ⇒ Task[Failure Either List[Package]]) {

    val httpRequest: Request = new Request(
      method  = Method.GET,
      uri     = baseUri
        .withPath(config.paths.search)
        .withQueryParam("c", "3")
        .withQueryParam("rt", "1")
        .withQueryParam("q", URLEncoder.encode(request.word, "UTF-8")),
      headers = headers.fullHeaders(auth)
    )

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests ⇒ QuotaExceeded(auth)
      case _ ⇒ GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Either List[Package]] =
      client.expect[ByteVector](httpRequest).map { bv ⇒
        val searchResponse = ResponseWrapper.parseFrom(bv.toArray).getPayload.getSearchResponse
        val packages = Converters.searchResponseToPackages(searchResponse)
        val filtered = packages.diff(request.excludedApps).take(request.maxTotal)
        Either.right(filtered)
      }.handle {
        case e: UnexpectedStatus ⇒ Either.left(handleUnexpected(e))
      }

  }

}
