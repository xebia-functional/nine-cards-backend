package cards.nine.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import cats.syntax.xor._
import cards.nine.googleplay.domain.{ GoogleAuthParams, Package }
import cards.nine.googleplay.proto.GooglePlay.{ ResponseWrapper, DocV2 }
import org.http4s.Http4s._
import org.http4s.Status.ResponseClass.Successful
import org.http4s.{ Method, Request, Response, Uri }
import org.http4s.client.Client
import scalaz.concurrent.Task
import scodec.bits.ByteVector

case class ApiClient(config: Configuration, client: Client) {

  private[this] val baseUri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(Uri.Authority(host = Uri.RegName(config.host)))
  )

  object details {

    val uri: Uri = baseUri.withPath(config.detailsPath)

    def apply(packageName: Package, auth: GoogleAuthParams): Task[Xor[Response, DocV2]] = {
      def toDocV2(byteVector: ByteVector): DocV2 =
        ResponseWrapper.parseFrom(byteVector.toArray).getPayload.getDetailsResponse.getDocV2

      val httpRequest: Request =
        new Request(
          method  = Method.GET,
          uri     = uri.withQueryParam("doc", packageName.value),
          headers = headers.fullHeaders(auth)
        )
      run[Response, DocV2](httpRequest, (r ⇒ r), toDocV2)
    }

  }

  private[this] def run[L, R](request: Request, failed: Response ⇒ L, success: ByteVector ⇒ R): Task[Xor[L, R]] = {
    client.fetch(request) {
      case Successful(resp) ⇒ resp.as[ByteVector].map(bv ⇒ success(bv).right[L])
      case resp ⇒ Task.now(failed(resp).left[R])
    }
  }

}