package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.domain.GooglePlayApp
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayApp = jsonFormat7(GooglePlayApp)
}
