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
package cards.nine.commons

import cards.nine.commons.NineCardsErrors.NineCardsError
import cats.Monad
import cats.arrow.FunctionK
import cats.data.EitherT
import cats.free.{ :<:, Free }
import cats.syntax.either._

object NineCardsService {

  type NineCardsService[F[_], A] = EitherT[Free[F, ?], NineCardsError, A]

  type Result[A] = NineCardsError Either A

  def apply[F[_], A](f: Free[F, Result[A]]): NineCardsService[F, A] = {
    EitherT[Free[F, ?], NineCardsError, A](f)
  }

  def apply[Ops[_], F[_], A](op: Ops[Result[A]])(implicit I: Ops :<: F): NineCardsService[F, A] =
    apply[F, A](Free.inject[Ops, F](op))

  def fromEither[F[_], A](e: Result[A]) = NineCardsService[F, A](Free.pure(e))

  def left[F[_], A](e: NineCardsError) = NineCardsService[F, A](Free.pure(Either.left(e)))

  def right[F[_], A](a: A): NineCardsService[F, A] = NineCardsService[F, A](Free.pure(Either.right(a)))

  implicit class NineCardsServiceOps[F[_], A](service: NineCardsService[F, A]) {

    def foldMap[M[_]](f: FunctionK[F, M])(implicit M: Monad[M]): M[Result[A]] =
      service.value.foldMap(f)
  }
}
