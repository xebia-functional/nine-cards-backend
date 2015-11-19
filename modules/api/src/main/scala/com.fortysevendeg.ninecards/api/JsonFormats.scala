package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayApp = jsonFormat7(GooglePlayApp)
}
