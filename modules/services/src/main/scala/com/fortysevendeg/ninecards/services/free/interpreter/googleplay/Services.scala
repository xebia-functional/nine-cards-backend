package com.fortysevendeg.ninecards.services.free.interpreter.googleplay

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay._
import org.http4s.Http4s._
import org.http4s.{ Header, Headers, Method, Request, Uri }
import org.http4s.Uri.{ Authority, RegName }

import scalaz.concurrent.Task

class Services(config: Configuration) {

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

  def resolveOne(packageName: String, auth: AuthParams): Task[String Xor AppInfo] = {
    val uri = Uri(
      scheme    = Option(config.protocol.ci),
      authority = Option(authority),
      path      = s"${config.resolveOneUri}/$packageName"
    )

    val request = Request(Method.GET, uri = uri, headers = authHeaders(auth))
    client.fetchAs[String Xor AppInfo](request)
  }

  def resolveMany(packageNames: Seq[String], auth: AuthParams): Task[AppsInfo] = {
    val resolveManyUri = Uri(
      scheme    = Option(config.protocol.ci),
      authority = Option(authority),
      path      = config.resolveManyUri
    )

    val request = Request(Method.POST, uri = resolveManyUri, headers = authHeaders(auth))
      .withBody[PackageList](PackageList(packageNames))
    client.expect[AppsInfo](request)
  }

}

object Services {

  def services(implicit config: Configuration) = new Services(config)
}
