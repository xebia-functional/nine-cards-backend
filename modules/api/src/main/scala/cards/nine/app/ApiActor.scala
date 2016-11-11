package cards.nine.api

import akka.actor.Actor
import cards.nine.commons.config.NineCardsConfig
import spray.routing.HttpService

import scala.concurrent.ExecutionContext

class NineCardsApiActor
  extends Actor
  with AuthHeadersRejectionHandler
  with HttpService
  with NineCardsExceptionHandler {

  override val actorRefFactory = context

  implicit val executionContext: ExecutionContext = actorRefFactory.dispatcher
  implicit val config = NineCardsConfig.nineCardsConfiguration

  def receive = runRoute(new NineCardsRoutes().nineCardsRoutes)

}
