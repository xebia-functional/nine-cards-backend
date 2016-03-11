package com.fortysevendeg.ninecards.services.free.interpreter.impl

import cats.data.Xor
import com.fortysevendeg.ninecards.services.free.domain.{TokenInfo, WrongTokenInfo}
import org.http4s.Http4s._
import org.http4s.Uri
import org.http4s.Uri.{Authority, RegName}

import scalaz.concurrent.Task

class GoogleApiServices(implicit config: GoogleApiConfiguration) extends GoogleApiDecoders {

  val client = org.http4s.client.blaze.PooledHttp1Client()

  def getTokenInfo(
    tokenId: String): Task[WrongTokenInfo Xor TokenInfo] = {
    val authority = Authority(host = RegName(config.host), port = config.port)

    val getTokenInfoUri = Uri(scheme = Option(config.protocol.ci), authority = Option(authority))
      .withPath(config.tokenInfoUri)
      .withQueryParam(config.tokenIdQueryParameter, tokenId)

    client.getAs[WrongTokenInfo Xor TokenInfo](getTokenInfoUri)
  }
}

object GoogleApiServices {

  implicit def googleApiServices(implicit config: GoogleApiConfiguration) = new GoogleApiServices
}
