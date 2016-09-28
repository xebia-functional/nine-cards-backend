package cards.nine.googleplay.service.free.interpreter.webscrapper

import cats.~>
import cats.data.Xor
import cards.nine.googleplay.domain.{FullCard, Package}
import cards.nine.googleplay.domain.webscrapper._
import cards.nine.googleplay.service.free.algebra.webscrapper._
import org.http4s.Http4s._
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Method, Request, Status, Uri}
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Interpreter(config: Configuration) extends (Ops ~> WithClient){

  override def apply[A]( ops: Ops[A]) : WithClient[A] = ops match {
    case ExistsApp(pack) => new ExistsAppWP(pack)
    case GetDetails(pack) => new GetDetailsWP(pack)
  }

  private[this] val baseUri = Uri(
    scheme = Option(config.protocol.ci),
    authority = Option( Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)) )
  )

  private[this] def detailsUriOf(pack: Package) = baseUri
    .withPath(config.detailsPath)
    .withQueryParam( "id", pack.value)
    .withQueryParam( "hl", "en_US")

  private[this] class ExistsAppWP(pack: Package) extends WithClient[Boolean] {
    override def apply(client: Client) : Task[Boolean] = {
      val request = new Request(method = Method.HEAD, uri = detailsUriOf(pack) )
      client.fetch(request)(resp => Task.now(resp.status.isSuccess))
    }
  }

  private[this] class GetDetailsWP(pack: Package) extends WithClient[Failure Xor FullCard] {

    def handleUnexpected(e: UnexpectedStatus): Failure = e.status match {
      case Status.NotFound ⇒ PackageNotFound(pack)
      case _ => WebPageServerError
    }

    override def apply(client: Client): Task[Failure Xor FullCard] = {
      val httpRequest = new Request(method = Method.GET, uri = detailsUriOf(pack) )

      client.expect[ByteVector](httpRequest).map { bv =>
        GooglePlayPageParser.parseCard(bv) match {
          case Some(card) => Xor.Right(card)
          case None => Xor.Left( PageParseFailed(pack))
        }
      }.handle {
        case e: UnexpectedStatus ⇒ Xor.Left(handleUnexpected(e))
      }
    }
  }

}
