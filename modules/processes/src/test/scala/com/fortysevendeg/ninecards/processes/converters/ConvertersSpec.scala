package com.fortysevendeg.ninecards.processes.converters

import com.fortysevendeg.ninecards.processes.messages.GooglePlayMessages.AuthParams
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay.{ AppInfo, AppsInfo }
import com.fortysevendeg.ninecards.services.free.domain.{ Installation, User }
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

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

  "toCategorizeAppsResponse" should {
    "convert an AppsInfo to a CategorizeAppsResponse object" in {
      prop { appsInfo: AppsInfo ⇒

        val (appsWithoutCategories, _) = appsInfo.apps.partition(app ⇒ app.categories.isEmpty)

        val categorizeAppsResponse = Converters.toCategorizeAppsResponse(appsInfo)

        categorizeAppsResponse.errors shouldEqual appsInfo.missing ++ appsWithoutCategories.map(_.packageName)

        forall(categorizeAppsResponse.items) { item ⇒
          appsInfo.apps.exists { app ⇒
            app.packageName == item.packageName &&
              app.categories.nonEmpty &&
              app.categories.head == item.category
          } should beTrue
        }
      }
    }
  }

  "toAuthParamsServices" should {
    "convert an AuthParams (processes) to an AuthParams (services) object" in {
      prop { authParams: AuthParams ⇒

        val authParamsServices = Converters.toAuthParamsServices(authParams)

        authParamsServices.androidId shouldEqual authParams.androidId
        authParamsServices.localization shouldEqual authParams.localization
        authParamsServices.token shouldEqual authParams.token
      }
    }
  }

}