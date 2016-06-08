package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.extracats.taskMonad
import com.fortysevendeg.ninecards.googleplay.domain.Domain.{Item, Package}
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import scalaz.concurrent.Task

class Http4sGooglePlayWebScraper( serverUrl: String, client: Client) extends QueryService {

  def apply(query: QueryRequest): Task[QueryResult] = {
    val packageName = query._1.value
    val (_,_,locOption) = query._2

    val urlString = {
      val localeParam = locOption.fold("")(l => s"&hl=${l.value}")
      s"$serverUrl?id=$packageName${localeParam}"
    }

    lazy val failed = packageName.left[Item]

    def runUri(u: Uri) : Task[QueryResult] = {
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
