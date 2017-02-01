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
package cards.nine.commons.catscalaz

import cats.{ Applicative, ApplicativeError, Monad }

import scalaz.concurrent.Task

trait TaskInstances {

  implicit val taskApplicative: Applicative[Task] = ScalazInstances[Task].applicativeInstance

  implicit val taskMonad: Monad[Task] with ApplicativeError[Task, Throwable] =
    new Monad[Task] with ApplicativeError[Task, Throwable] {

      val monadInstance = ScalazInstances[Task].monadInstance

      override def raiseError[A](e: Throwable): Task[A] = Task.fail(e)

      override def handleErrorWith[A](fa: Task[A])(f: (Throwable) ⇒ Task[A]): Task[A] =
        fa.handleWith({ case x ⇒ f(x) })

      override def flatMap[A, B](fa: Task[A])(f: (A) ⇒ Task[B]): Task[B] = monadInstance.flatMap(fa)(f)

      override def tailRecM[A, B](a: A)(f: (A) ⇒ Task[Either[A, B]]): Task[B] = monadInstance.tailRecM(a)(f)

      override def pure[A](x: A): Task[A] = monadInstance.pure(x)
    }
}

object TaskInstances extends TaskInstances