package cards.nine.app

import akka.actor.Actor
import akka.event.LoggingAdapter
import cards.nine.commons.NineCardsService._
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.domain.application.ResolvePendingStats
import cards.nine.processes.ApplicationProcesses
import cats.~>
import scalaz.{ -\/, \/, \/- }
import scalaz.concurrent.Task

class AppResolverActor[F[_]](interpreter: F ~> Task, log: LoggingAdapter)(implicit process: ApplicationProcesses[F]) extends Actor {

  import AppResolverMessages._
  val numAppsToResolve = nineCardsConfiguration.google.play.resolveBatchSize

  def receive = {
    case ResolveApps ⇒
      log.info("Running Apps Resolver Actor to resolve pending apps...")
      process
        .resolvePendingApps(numAppsToResolve)
        .foldMap(interpreter)
        .unsafePerformAsync(showAppResolutionInfo)
  }

  private[this] def showAppResolutionInfo(result: Throwable \/ Result[ResolvePendingStats]) =
    result match {
      case -\/(e) ⇒ log.error(e, "An error was found while resolving pending packages")
      case \/-(Left(_)) ⇒ log.error("An error was found while resolving pending packages")
      case \/-(Right(stats)) ⇒
        log.info(s"The server resolved ${stats.resolved} packages from the pending queue to an App")
        log.info(s"The server resolved ${stats.errors} pending packages not to exist in the App Store")
        log.info(s"The server returned ${stats.pending} packages to the pending queue")
    }

}

object AppResolverMessages {
  val ResolveApps = "resolveApps"
}
