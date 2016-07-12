package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.extracats.taskMonad
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item, Localization}
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import scalaz.concurrent.Task

class Http4sGooglePlayWebScraper(serverUrl: String, client: Client) extends AppService {

  def apply(appRequest: AppRequest): Task[Xor[String, Item]] = {
    val packageName: String = appRequest.packageName.value

    val urlString = {
      val localeQueryParam = appRequest.authParams.localization.fold("")(l => s"&hl=${l.value}")
      s"$serverUrl?id=$packageName${localeQueryParam}"
    }

    lazy val failed = packageName.left[Item]

    def runUri(u: Uri) : Task[Xor[String, Item]] = {
      val request = new Request( method = Method.GET, uri = u )
      client.fetch(request) {
        case Successful(resp) =>
          import ItemDecoders.itemOptionDecoder
          resp.as[Option[Item]].map( n => n.fold(failed)(i => i.right[String]) )
        case _ => Task.now(failed)
      }
    }

    Uri.fromString(urlString).fold( _ => Task.now(failed), runUri)
  }

}
