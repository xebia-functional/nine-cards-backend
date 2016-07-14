package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import cats.data.Xor
import cats.syntax.xor._
import com.fortysevendeg.extracats.taskMonad
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item, Package, GoogleAuthParams, Localization}
import org.http4s.Status.ResponseClass.Successful
import org.http4s.client.Client
import org.http4s.{Header, Headers, Method, Request, Uri}
import scalaz.concurrent.Task

class Http4sGooglePlayApiClient(serverUrl: String, client: Client) extends AppService {

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

  private[this] def buildRequest(uri: Uri, auth: GoogleAuthParams) : Request = {
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
    new Request( method = Method.GET, uri = uri, headers = Headers( variableHeaders ++ fixedHeaders) )
  }

  def apply(appRequest: AppRequest): Task[Xor[String, Item]] = {
    val packageName = appRequest.packageName.value
    lazy val failed = packageName.left[Item]

    def runRequest(u: Uri) : Task[Xor[String,Item]] = {
      val request = buildRequest(u,  appRequest.authParams)
      client.fetch(request) {
        case Successful(resp) =>
          import ItemDecoders.protobufItemDecoder
          resp.as[Item].map(i => i.right[String])
        case _ => Task.now(failed)
      }.handle {
        case _ => failed
      }
    }

    Uri.fromString(s"${serverUrl}?doc=$packageName").fold( _ => Task.now(failed), runRequest)
  }

}
