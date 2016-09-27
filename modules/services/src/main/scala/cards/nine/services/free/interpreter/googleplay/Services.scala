package cards.nine.services.free.interpreter.googleplay

import cards.nine.services.free.algebra.GooglePlay._
import cats.data.Xor
import cards.nine.services.free.domain.GooglePlay._
import cats.~>
import org.http4s.Http4s._
import org.http4s._
import org.http4s.Uri.{ Authority, RegName }

import scalaz.concurrent.Task

class Services(config: Configuration) extends (Ops ~> Task) {

  import Encoders._
  import Decoders._

  private[this] val client = org.http4s.client.blaze.PooledHttp1Client()

  private[this] val authority = Authority(host = RegName(config.host), port = config.port)

  private[this] def authHeaders(auth: AuthParams): Headers = {
    val locHeader: List[Header] = auth.localization
      .map(Header("X-Android-Market-Localization", _))
      .toList

    Headers(
      Header("X-Google-Play-Token", auth.token)
        :: Header("X-Android-ID", auth.androidId)
        :: locHeader
    )
  }

  private[this] val recommendationsBaseUri = Uri(
    scheme    = Option(config.protocol.ci),
    authority = Option(authority),
    path      = config.recommendationsPath
  )

  def resolveOne(packageName: String, auth: AuthParams): Task[String Xor AppInfo] = {
    val uri = Uri(
      scheme    = Option(config.protocol.ci),
      authority = Option(authority),
      path      = s"${config.resolveOnePath}/$packageName"
    )

    val request = Request(Method.GET, uri = uri, headers = authHeaders(auth))
    client.fetchAs[String Xor AppInfo](request)
  }

  def resolveMany(packageNames: Seq[String], auth: AuthParams): Task[AppsInfo] = {
    val resolveManyUri = Uri(
      scheme    = Option(config.protocol.ci),
      authority = Option(authority),
      path      = config.resolveManyPath
    )

    val request = Request(Method.POST, uri = resolveManyUri, headers = authHeaders(auth))
      .withBody[PackageList](PackageList(packageNames))
    client.expect[AppsInfo](request)
  }

  def recommendByCategory(category: String, filter: String, auth: AuthParams): Task[Recommendations] = {

    val uri = recommendationsBaseUri./(category)./(filter)

    val request = Request(Method.GET, uri = uri, headers = authHeaders(auth))
    client.expect[Recommendations](request)
  }

  def recommendationsForApps(packageNames: Seq[String], auth: AuthParams): Task[Recommendations] = {

    val request = Request(Method.POST, uri = recommendationsBaseUri, headers = authHeaders(auth))
      .withBody[PackageList](PackageList(packageNames))

    client.expect[Recommendations](request)
  }

  def apply[A](fa: Ops[A]): Task[A] = fa match {
    case ResolveMany(packageNames, auth) ⇒
      resolveMany(packageNames, auth)
    case Resolve(packageName, auth) ⇒
      resolveOne(packageName, auth)
    case RecommendationsByCategory(category, filter, auth) ⇒
      recommendByCategory(category, filter, auth)
    case RecommendationsForApps(packagesName, auth) ⇒
      recommendationsForApps(packagesName, auth)
  }
}

object Services {

  def services(implicit config: Configuration) = new Services(config)
}
