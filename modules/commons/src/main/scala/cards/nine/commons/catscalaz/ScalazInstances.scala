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

import cats.{ Applicative, Monad }

import scalaz.\/

class ScalazInstances[F[_]](implicit A: scalaz.Applicative[F], M: scalaz.Monad[F], B: scalaz.BindRec[F]) {

  val applicativeInstance: Applicative[F] = new Applicative[F] {

    override def pure[A](x: A): F[A] = A.pure(x)

    override def ap[A, B](ff: F[(A) ⇒ B])(fa: F[A]): F[B] = A.ap(fa)(ff)
  }

  val monadInstance: Monad[F] = new Monad[F] {

    override def pure[A](x: A): F[A] = M.pure(x)

    override def flatMap[A, B](fa: F[A])(f: (A) ⇒ F[B]): F[B] = M.bind(fa)(f)

    override def tailRecM[A, B](a: A)(f: (A) ⇒ F[Either[A, B]]): F[B] =
      B.tailrecM(f.andThen(e ⇒ M.map(e)(\/.fromEither)))(a)
  }
}

object ScalazInstances {
  def apply[F[_]](implicit A: scalaz.Applicative[F], M: scalaz.Monad[F], B: scalaz.BindRec[F]): ScalazInstances[F] =
    new ScalazInstances[F]
}
