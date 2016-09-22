package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.webscrapper

import cats.~>
import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain.{FullCard, Package}
import com.fortysevendeg.ninecards.googleplay.domain.webscrapper.Failure
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.webscrapper._

trait InterpreterServer[F[_]] {
  def existsApp(pack: Package): F[Boolean]
  def getDetails(pack: Package): F[Failure Xor FullCard]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A]( ops: Ops[A]) = ops match {
    case ExistsApp(pack) => server.existsApp(pack)
    case GetDetails(pack) => server.getDetails(pack)
  }

}

