package cards.nine.processes.account

import cards.nine.services.free.domain._
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  import Converters._

  "toLoginResponse" should {
    "convert an User to a LoginResponse object" in {
      prop { info: (User, Installation) ⇒

        val processLoginResponse = toLoginResponse(info)
        processLoginResponse.sessionToken shouldEqual info._1.sessionToken
      }
    }
  }

  "toUpdateInstallationResponse" should {
    "convert an Installation to a UpdateInstallationResponse object" in {
      prop { installation: Installation ⇒

        val processUpdateInstallationResponse = toUpdateInstallationResponse(installation)
        processUpdateInstallationResponse.androidId shouldEqual installation.androidId
        processUpdateInstallationResponse.deviceToken shouldEqual installation.deviceToken
      }
    }
  }

}
