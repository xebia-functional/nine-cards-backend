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
