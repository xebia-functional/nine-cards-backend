/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.app

import akka.actor.Actor
import akka.event.LoggingAdapter
import cards.nine.commons.NineCardsService._
import cards.nine.commons.catscalaz.TaskInstances._
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.domain.application.ResolvePendingStats
import cards.nine.processes.applications.ApplicationProcesses
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