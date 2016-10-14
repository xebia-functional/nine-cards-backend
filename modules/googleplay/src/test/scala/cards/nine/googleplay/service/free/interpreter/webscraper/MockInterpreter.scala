package cards.nine.googleplay.service.free.interpreter.webscrapper

import cats.~>
import cats.data.Xor
import cards.nine.domain.application.Package
import cards.nine.googleplay.domain.FullCard
import cards.nine.googleplay.domain.webscrapper.Failure
import cards.nine.googleplay.service.free.algebra.WebScraper._

trait InterpreterServer[F[_]] {
  def existsApp(pack: Package): F[Boolean]
  def getDetails(pack: Package): F[Failure Xor FullCard]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A](ops: Ops[A]) = ops match {
    case ExistsApp(pack) ⇒ server.existsApp(pack)
    case GetDetails(pack) ⇒ server.getDetails(pack)
  }

}

