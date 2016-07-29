package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.ninecards.googleplay.domain._
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Header, Headers, Method, Request, Uri}
import scalaz.concurrent.Task
import scodec.bits.ByteVector

class Http4sGooglePlayApiClient(serverUrl: String, client: Client) {

  private[this] val fixedHeaders = List(
    Header("Content-Type", "application/json; charset=UTF-8"),
    Header("Host", "android.clients.google.com"),
    Header("User-Agent", "Android-Finsky/3.10.14 (api=3,versionCode=8016014,sdk=15,device=GT-I9300,hardware=aries,product=GT-I9300)"),
    Header("X-DFE-Client-Id", "am-android-google"),
    Header("X-DFE-Enabled-Experiments", "cl:billing.select_add_instrument_by_default"),
    Header("X-DFE-Filter-Level", "3"),
    Header("X-DFE-SmallestScreenWidthDp", "320"),
    Header("X-DFE-Unsupported-Experiments", "nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes")
  )

  private[this] def buildHeaders(auth: GoogleAuthParams) : Headers = {
    val variableHeaders: List[Header] = {
      val GoogleAuthParams(androidId, token, local) = auth
      Header("Authorization", s"GoogleLogin auth=${token.value}") ::
      Header("X-DFE-Device-Id", androidId.value) :: (
        local match {
          case Some(Localization(locale)) => List( Header("Accept-Language", locale) )
          case None => List()
        }
      )
    }
    Headers( variableHeaders ++ fixedHeaders)
  }

  private[this] def runRequest[L,R](
    appRequest: AppRequest,
    failed: => L,
    parserR: ByteVector => Xor[L,R]
  ): Task[Xor[L,R]] = {
    val uriString = s"${serverUrl}?doc=${appRequest.packageName.value}"
    Uri.fromString(uriString).toOption match {
      case Some(uri) =>
        val headers = buildHeaders(appRequest.authParams)
        val request = new Request( method = Method.GET, uri = uri, headers = headers )

        client.fetch( request ) {
          case Successful(resp) =>
            resp.as[ByteVector].map(parserR)
          case _ =>
            Task.now(failed.left[R])
        }.handle {
          case _ => failed.left[R]
        }

      case None => Task.now(failed.left[R])
    }
  }

  def getItem(appRequest: AppRequest): Task[Xor[String, Item]] = runRequest[String,Item](
    appRequest = appRequest,
    failed = appRequest.packageName.value,
    parserR = (bv => GoogleApiItemParser.parseItem(bv).right[String] )
  )

  def getCard(appRequest: AppRequest): Task[Xor[InfoError, AppCard]] = {
    val failed = InfoError(appRequest.packageName.value)
    runRequest[InfoError,AppCard]( appRequest, failed, {bv =>
      GoogleApiItemParser.parseCard(bv).right[InfoError]
    })
  }

}
