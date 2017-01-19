package cards.nine.api.accounts

import cards.nine.api.NineCardsDirectives._
import cards.nine.api.NineCardsHeaders.Domain._
import cards.nine.api.accounts.messages._
import cards.nine.api.utils.SprayMarshallers._
import cards.nine.commons.config.Domain.NineCardsConfiguration
import cards.nine.domain.account.SessionToken
import cards.nine.processes._
import cards.nine.processes.account.AccountProcesses
import cards.nine.processes.NineCardsServices._
import scala.concurrent.ExecutionContext
import spray.httpx.SprayJsonSupport
import spray.routing._

class AccountsApi(
  implicit
  config: NineCardsConfiguration,
  accountProcesses: AccountProcesses[NineCardsServices],
  executionContext: ExecutionContext
) extends SprayJsonSupport {

  import Converters._
  import Directives._
  import JsonFormats._

  lazy val route: Route =
    pathPrefix("installations")(installationsRoute) ~
      pathPrefix("login")(userRoute)

  private[this] lazy val userRoute: Route =
    pathEndOrSingleSlash {
      post {
        entity(as[ApiLoginRequest]) { request ⇒
          nineCardsDirectives.authenticateLoginRequest { sessionToken: SessionToken ⇒
            complete {
              accountProcesses
                .signUpUser(toLoginRequest(request, sessionToken))
                .map(toApiLoginResponse)
            }
          }
        }
      }
    }

  private[this] lazy val installationsRoute: Route =
    nineCardsDirectives.authenticateUser { implicit userContext: UserContext ⇒
      pathEndOrSingleSlash {
        put {
          entity(as[ApiUpdateInstallationRequest]) { request ⇒
            complete {
              accountProcesses
                .updateInstallation(toUpdateInstallationRequest(request, userContext))
                .map(toApiUpdateInstallationResponse)
            }
          }
        }
      }
    }

}
