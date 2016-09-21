package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.~>
import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain.{Details => _, _}
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.apigoogle._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi.proto.GooglePlay.{ResponseWrapper, DocV2}
import org.http4s.Http4s._
import org.http4s.{Method, Request, Status, Uri}
import org.http4s.client.{Client, UnexpectedStatus}
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Interpreter(config: Configuration) extends (Ops ~> WithHttpClient)  {

  def apply[A]( ops: Ops[A]): WithHttpClient[A] = ops match {

    case GetDetails(pack, auth) => new DetailsWithClient(pack, auth)

  }

  private[this] val baseUri = Uri(
    scheme = Option(config.protocol.ci),
    authority = Option( Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)) )
  )

  class DetailsWithClient(packageName: Package, auth: GoogleAuthParams)
      extends (Client => Task[Failure Xor FullCard]) {

    val httpRequest: Request =
      new Request(
        method = Method.GET,
        uri = baseUri
          .withPath(config.detailsPath)
          .withQueryParam( "doc", packageName.value),
        headers = headers.fullHeaders(auth)
      )

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.NotFound ⇒ PackageNotFound(packageName)
      case Status.Unauthorized ⇒ WrongAuthParams(auth)
      case Status.TooManyRequests => QuotaExceeded(auth)
      case _ => GoogleApiServerError
    }

    def apply(client: Client): Task[Failure Xor FullCard] =
      client.expect[ByteVector](httpRequest).map { bv =>
        val docV2: DocV2 = ResponseWrapper.parseFrom(bv.toArray).getPayload.getDetailsResponse.getDocV2
        val fullCard = Converters.toFullCard(docV2)
        Xor.Right(fullCard)
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(handleUnexpected(e))
      }
  }


}
