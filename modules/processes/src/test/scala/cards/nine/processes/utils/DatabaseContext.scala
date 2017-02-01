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
package cards.nine.processes.utils

import cards.nine.commons.config.DummyConfig
import cards.nine.services.persistence.DatabaseTransactor
import cats.Id
import doobie.imports.Transactor
import doobie.util.capture.Capture

import scalaz.{ Catchable, Monad, \/ }

trait CatsIdInstances {

  implicit val idMonad = new Monad[Id] {
    override def bind[A, B](fa: Id[A])(f: (A) ⇒ Id[B]): Id[B] = f(fa)

    override def point[A](a: ⇒ A): Id[A] = a
  }

  implicit val idCatchable = new Catchable[Id] {

    override def attempt[A](f: Id[A]): Id[\/[Throwable, A]] = \/.fromTryCatchNonFatal(f)

    override def fail[A](err: Throwable): Id[A] = throw err
  }

  implicit val idCapture = new Capture[Id] {
    override def apply[A](a: ⇒ A): Id[A] = a
  }
}

object DatabaseContext extends DummyConfig with CatsIdInstances {

  implicit val transactor: Transactor[Id] = new DatabaseTransactor(config.db).transactor
}
