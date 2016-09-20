package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import cats.~>
import com.fortysevendeg.ninecards.googleplay.domain.Package
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._

trait InterpreterServer[F[_]] {
  def existsApp(pack: Package): F[Boolean]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A]( ops: Ops[A]) = ops match {
    case ExistsApp(pack) => server.existsApp(pack)
  }

}

