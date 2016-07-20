package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, AppCard, Localization, AppMissed, Item}
import org.http4s.{Method, Request, Response, Uri}
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Http4sGooglePlayWebScraper(serverUrl: String, client: Client) {

  private[this] def buildRequest( appRequest: AppRequest) : Option[Request] = {
    val packageName: String = appRequest.packageName.value
    val locale = appRequest.authParams.localization.fold("")(l => s"&hl=${l.value}")
    val uriString = s"${serverUrl}?id=${packageName}${locale}"

    for /*Option*/ {
      uri <- Uri.fromString(uriString).toOption
      request = new Request( method = Method.GET, uri = uri )
    } yield request
  }

  private[this] def runRequest[L,R](
    appRequest: AppRequest,
    failed: => L,
    parserR: ByteVector => Xor[L,R]
  ): Task[Xor[L,R]] = {
    lazy val leftFailed = Xor.Left(failed)
    buildRequest(appRequest) match {
      case Some(request) =>
        client.fetch(request){
          case Successful(resp) =>
            resp.as[ByteVector].map(parserR)
          case _ =>
            Task.now(failed.left[R])
        }.handle {
          case _ => failed.left[R]
        }

      case None => Task.now(leftFailed)
    }
  }

  def getItem(appRequest: AppRequest): Task[Xor[String, Item]] = {
    lazy val failed = appRequest.packageName.value
    runRequest[String,Item](appRequest, failed, { bv =>
      GooglePlayPageParser.parseItem(bv)
        .fold(failed.left[Item])(i => i.right[String])
    })
  }

  def getCard(appRequest: AppRequest): Task[Xor[AppMissed, AppCard]] = {
    lazy val failed: AppMissed = AppMissed(
      packageName = appRequest.packageName.value,
      cause = "Could not find the Package"
    )
    runRequest[AppMissed, AppCard](appRequest, failed, { bv =>
      GooglePlayPageParser.parseCard(bv)
        .fold(failed.left[AppCard])(i => i.right[AppMissed])
    })
  }
}
