package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.services.free.domain.{User, Installation, GooglePlayApp}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.scalacheck.Shapeless._

class ConvertersSpec
    extends Specification
    with ScalaCheck {

  "toLoginResponse" should {
    "convert an User to a LoginResponse object" in {
      prop { info: (User, Installation) ⇒

        val processLoginResponse = Converters.toLoginResponse(info)
        processLoginResponse.sessionToken shouldEqual info._1.sessionToken
      }
    }
  }

  "toUpdateInstallationResponse" should {
    "convert an Installation to a UpdateInstallationResponse object" in {
      prop { installation: Installation ⇒

        val processUpdateInstallationResponse = Converters.toUpdateInstallationResponse(installation)
        processUpdateInstallationResponse.androidId shouldEqual installation.androidId
        processUpdateInstallationResponse.deviceToken shouldEqual installation.deviceToken
      }
    }
  }

  "toGooglePlayApp" should {
    "convert an GooglePlayAppServices to a GooglePlayApp object" in {
      prop { app: GooglePlayApp ⇒

        val processGooglePlayApp = Converters.toGooglePlayApp(app)
        processGooglePlayApp.packageName shouldEqual app.packageName
      }
    }
  }
}