package  com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import cats.~>
import com.fortysevendeg.ninecards.googleplay.domain.Package
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._
import org.http4s.Http4s._
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import scalaz.concurrent.Task

class Interpreter(config: Configuration, httpClient: Client) extends (Ops ~> Task){

  def apply[A]( ops: Ops[A]) : Task[A] = ops match {
    case ExistsApp(pack) => existsApp(pack)
  }

  private[this] val baseUri = Uri(
    scheme = Option(config.protocol.ci),
    authority = Option( Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)) )
  )

  private[this] def detailsUriOf(pack: Package) = baseUri
    .withPath(config.detailsPath)
    .withQueryParam( "id", pack.value)
    .withQueryParam( "hl", "en_US")

  private[this] def existsApp(pack: Package): Task[Boolean] = {
    val request = new Request(method = Method.HEAD, uri = detailsUriOf(pack) )
    httpClient.fetch(request)(resp => Task.now(resp.status.isSuccess ) )
  }

}
