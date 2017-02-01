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
package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.service.free.algebra.Cache._
import cats.~>

trait InterpreterServer[F[_]] {
  def getValid(pack: Package): F[Option[FullCard]]
  def getValidMany(packages: List[Package]): F[List[FullCard]]
  def putResolved(card: FullCard): F[Unit]
  def putResolvedMany(cards: List[FullCard]): F[Unit]
  def putPermanent(card: FullCard): F[Unit]
  def setToPending(pack: Package): F[Unit]
  def setToPendingMany(packages: List[Package]): F[Unit]
  def addError(pack: Package): F[Unit]
  def addErrorMany(packages: List[Package]): F[Unit]
  def listPending(num: Int): F[List[Package]]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case GetValid(pack) ⇒ server.getValid(pack)
    case GetValidMany(packages) ⇒ server.getValidMany(packages)
    case PutResolved(card) ⇒ server.putResolved(card)
    case PutResolvedMany(packages) ⇒ server.putResolvedMany(packages)
    case PutPermanent(card) ⇒ server.putPermanent(card)
    case SetToPending(pack) ⇒ server.setToPending(pack)
    case SetToPendingMany(packages) ⇒ server.setToPendingMany(packages)
    case AddError(pack) ⇒ server.addError(pack)
    case AddErrorMany(packages) ⇒ server.addErrorMany(packages)
    case ListPending(num) ⇒ server.listPending(num)
  }

}
