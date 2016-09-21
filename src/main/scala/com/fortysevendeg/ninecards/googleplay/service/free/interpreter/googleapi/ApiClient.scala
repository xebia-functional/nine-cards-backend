package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.ninecards.googleplay.domain.{Category, GoogleAuthParams, Package, PriceFilter}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi.proto.GooglePlay.{ResponseWrapper, ListResponse, DocV2}
import org.http4s.Http4s._
import org.http4s.Status.ResponseClass.Successful
import org.http4s.{Method, Query, Request, Response, Uri}
import org.http4s.client.Client
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class ApiClient(config: Configuration, client: Client ) {

  private[this] val baseUri = Uri(
    scheme = Option(config.protocol.ci),
    authority = Option(Uri.Authority(host = Uri.RegName(config.host) ))
  )

  object details {

    val uri: Uri = baseUri.withPath(config.detailsPath)

    def apply( packageName: Package, auth: GoogleAuthParams ) : Task[Xor[Response, DocV2]] = {
      def toDocV2(byteVector: ByteVector) : DocV2 =
        ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2

      val httpRequest: Request =
        new Request(
          method = Method.GET,
          uri = uri.withQueryParam( "doc", packageName.value),
          headers = headers.fullHeaders(auth)
        )
      run[Response,DocV2](httpRequest, (r => r), toDocV2)
    }

  }

  object list {

    val uri: Uri = baseUri.withPath(config.listPath)

    def apply( category: Category, priceFilter: PriceFilter, auth: GoogleAuthParams ): Task[Xor[Response, ListResponse]] = {

      val subCategory: String = priceFilter match {
        case PriceFilter.FREE => "apps_topselling_free"
        case PriceFilter.PAID => "apps_topselling_paid"
        case PriceFilter.ALL => "apps_topgrossing"
      }

      val query: Query = Query.fromPairs(
        ( "c", "3" ),
        ( "cat", category.entryName ),
        ( "ctr",  subCategory )
      )

      val httpRequest: Request = new Request(
        method = Method.GET,
        uri = uri.copy( query = query),
        headers = headers.fullHeaders(auth)
      )

      run[Response, ListResponse]( httpRequest, (r => r), toListResponse)
    }
  }

  object recommendations {
    val uri: Uri = baseUri.withPath(config.recommendationsPath)

    def apply( packageName: Package, auth: GoogleAuthParams) : Task[Xor[Response, ListResponse]] = {
      val query: Query = Query.fromPairs(
        ("c", "3"),
        ("rt", "1"),
        ("doc", packageName.value)
      )
      val httpRequest: Request = new Request(
        method = Method.GET,
        uri = uri.copy(query = query),
        headers = headers.fullHeaders(auth)
      )
      run[Response, ListResponse](httpRequest, (r => r), toListResponse)
    }
  }

  private[this] def run[L,R](request: Request, failed: Response => L, success: ByteVector => R ): Task[Xor[L,R]] = {
    client.fetch(request) {
      case Successful(resp) => resp.as[ByteVector].map( bv => success(bv).right[L] )
      case resp => Task.now(failed(resp).left[R])
    }
  }

  private[this] def toListResponse(bv: ByteVector) =
    ResponseWrapper.parseFrom(bv.toArray).getPayload.getListResponse

}