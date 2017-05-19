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
package cards.nine.processes

import cards.nine.services.free.algebra._
import cards.nine.services.free.interpreter.Interpreters._
import cats.data.Coproduct
import cats.{ ApplicativeError, Monad, ~> }

import scalaz.concurrent.Task

object NineCardsServices {

  implicit val taskMonadInstance: Monad[Task] with ApplicativeError[Task, Throwable] = taskMonad

  type NineCardsServicesC08[A] = Coproduct[GoogleOAuth.Ops, User.Ops, A]
  type NineCardsServicesC07[A] = Coproduct[Subscription.Ops, NineCardsServicesC08, A]
  type NineCardsServicesC06[A] = Coproduct[Collection.Op, NineCardsServicesC07, A]
  type NineCardsServicesC05[A] = Coproduct[Ranking.Ops, NineCardsServicesC06, A]
  type NineCardsServicesC04[A] = Coproduct[Country.Ops, NineCardsServicesC05, A]
  type NineCardsServicesC03[A] = Coproduct[GooglePlay.Ops, NineCardsServicesC04, A]
  type NineCardsServicesC02[A] = Coproduct[GoogleApi.Ops, NineCardsServicesC03, A]
  type NineCardsServicesC01[A] = Coproduct[GoogleAnalytics.Ops, NineCardsServicesC02, A]
  type NineCardsServices[A] = Coproduct[Firebase.Ops, NineCardsServicesC01, A]

  class NineCardsInterpreters {

    val interpretersC08: NineCardsServicesC08 ~> Task =
      taskInterpreters.googleOAuthInterpreter or taskInterpreters.userInterpreter

    val interpretersC07: NineCardsServicesC07 ~> Task =
      taskInterpreters.subscriptionInterpreter or interpretersC08

    val interpretersC06: NineCardsServicesC06 ~> Task =
      taskInterpreters.collectionInterpreter or interpretersC07

    val interpretersC05: NineCardsServicesC05 ~> Task =
      taskInterpreters.rankingInterpreter or interpretersC06

    val interpretersC04: NineCardsServicesC04 ~> Task =
      taskInterpreters.countryInterpreter or interpretersC05

    val interpretersC03: NineCardsServicesC03 ~> Task =
      taskInterpreters.googlePlayInterpreter or interpretersC04

    val interpretersC02: NineCardsServicesC02 ~> Task =
      taskInterpreters.googleApiInterpreter or interpretersC03

    val interpretersC01: NineCardsServicesC01 ~> Task =
      taskInterpreters.analyticsInterpreter or interpretersC02

    val interpreters: NineCardsServices ~> Task =
      taskInterpreters.firebaseInterpreter or interpretersC01
  }

  val prodInterpreters: NineCardsServices ~> Task = new NineCardsInterpreters().interpreters
}