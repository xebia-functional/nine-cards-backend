package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.service.free.algebra.Cache._
import cats.~>

trait InterpreterServer[F[_]] {
  def getValid(pack: Package): F[Option[FullCard]]
  def getValidMany(packages: List[Package]): F[List[FullCard]]
  def putResolved(card: FullCard): F[Unit]
  def putResolvedMany(cards: List[FullCard]): F[Unit]
  def markPending(pack: Package): F[Unit]
  def markPendingMany(packages: List[Package]): F[Unit]
  def unmarkPending(pack: Package): F[Unit]
  def unmarkPendingMany(packages: List[Package]): F[Unit]
  def markError(pack: Package): F[Unit]
  def markErrorMany(packages: List[Package]): F[Unit]
  def clearInvalid(pack: Package): F[Unit]
  def clearInvalidMany(packages: List[Package]): F[Unit]
  def isPending(pack: Package): F[Boolean]
  def listPending(num: Int): F[List[Package]]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case GetValid(pack) ⇒ server.getValid(pack)
    case GetValidMany(packages) ⇒ server.getValidMany(packages)
    case PutResolved(card) ⇒ server.putResolved(card)
    case PutResolvedMany(packages) ⇒ server.putResolvedMany(packages)
    case MarkPending(pack) ⇒ server.markPending(pack)
    case MarkPendingMany(packages) ⇒ server.markPendingMany(packages)
    case UnmarkPending(pack) ⇒ server.unmarkPending(pack)
    case UnmarkPendingMany(packages) ⇒ server.unmarkPendingMany(packages)
    case MarkError(pack) ⇒ server.markError(pack)
    case MarkErrorMany(packages) ⇒ server.markErrorMany(packages)
    case ClearInvalid(pack) ⇒ server.clearInvalid(pack)
    case ClearInvalidMany(packages) ⇒ server.clearInvalidMany(packages)
    case IsPending(pack) ⇒ server.isPending(pack)
    case ListPending(num) ⇒ server.listPending(num)
  }

}
