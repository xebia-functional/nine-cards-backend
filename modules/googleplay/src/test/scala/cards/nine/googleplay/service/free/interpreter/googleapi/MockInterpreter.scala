package cards.nine.googleplay.service.free.interpreter.googleapi

import cats.~>
import cats.data.Xor
import cards.nine.googleplay.domain.{ FullCard, GoogleAuthParams, Package }
import cards.nine.googleplay.domain.apigoogle.{ Failure }
import cards.nine.googleplay.service.free.algebra.GoogleApi._

trait InterpreterServer[F[_]] {
  def getDetails(pack: Package, auth: GoogleAuthParams): F[Failure Xor FullCard]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case GetDetails(pack, auth) ⇒ server.getDetails(pack, auth)
  }

}

