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

import cards.nine.processes.NineCardsServices._
import cats._

import scalaz.concurrent.Task

trait TestInterpreters {

  val task2Id: (Task ~> Id) = new (Task ~> Id) {
    override def apply[A](fa: Task[A]): Id[A] = fa.unsafePerformSync
  }

  val testInterpreters: NineCardsServices ~> Id =
    new NineCardsInterpreters().interpreters.andThen(task2Id)
}
