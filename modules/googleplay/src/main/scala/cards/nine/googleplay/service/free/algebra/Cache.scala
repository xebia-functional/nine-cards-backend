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
package cards.nine.googleplay.service.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.domain.application.{ FullCard, Package }

object Cache {

  sealed trait Ops[A]

  case class GetValid(pack: Package) extends Ops[Option[FullCard]]

  case class GetValidMany(packages: List[Package]) extends Ops[List[FullCard]]

  case class PutResolved(card: FullCard) extends Ops[Unit]

  case class PutResolvedMany(cards: List[FullCard]) extends Ops[Unit]

  case class PutPermanent(card: FullCard) extends Ops[Unit]

  case class SetToPending(pack: Package) extends Ops[Unit]

  case class SetToPendingMany(packages: List[Package]) extends Ops[Unit]

  case class AddError(`package`: Package) extends Ops[Unit]

  case class AddErrorMany(packages: List[Package]) extends Ops[Unit]

  case class ListPending(limit: Int) extends Ops[List[Package]]

  class Service[F[_]](implicit I: Inject[Ops, F]) {

    def getValid(pack: Package): Free[F, Option[FullCard]] = Free.inject[Ops, F](GetValid(pack))

    def getValidMany(packages: List[Package]): Free[F, List[FullCard]] =
      Free.inject[Ops, F](GetValidMany(packages))

    def putResolved(card: FullCard): Free[F, Unit] = Free.inject[Ops, F](PutResolved(card))

    def putResolvedMany(cards: List[FullCard]): Free[F, Unit] =
      Free.inject[Ops, F](PutResolvedMany(cards))

    def putPermanent(card: FullCard): Free[F, Unit] =
      Free.inject[Ops, F](PutPermanent(card))

    def setToPending(pack: Package): Free[F, Unit] =
      Free.inject[Ops, F](SetToPending(pack))

    def setToPendingMany(packages: List[Package]): Free[F, Unit] =
      Free.inject[Ops, F](SetToPendingMany(packages))

    def addError(pack: Package): Free[F, Unit] = Free.inject[Ops, F](AddError(pack))

    def addErrorMany(packages: List[Package]): Free[F, Unit] = Free.inject[Ops, F](AddErrorMany(packages))

    def listPending(limit: Int): Free[F, List[Package]] = Free.inject[Ops, F](ListPending(limit))

  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}