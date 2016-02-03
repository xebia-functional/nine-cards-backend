package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.messages._
import com.fortysevendeg.ninecards.processes.domain._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonFormats
  extends DefaultJsonProtocol
  with SprayJsonSupport {

  implicit val googlePlayAppFormat = jsonFormat7(GooglePlayApp)

  implicit val userFormat = jsonFormat1(User)

  implicit val addUserRequest = jsonFormat3(AddUserRequest)

  implicit val installationFormat = jsonFormat3(Installation)

  implicit val installationRequestFormat = jsonFormat2(InstallationRequest)

}
