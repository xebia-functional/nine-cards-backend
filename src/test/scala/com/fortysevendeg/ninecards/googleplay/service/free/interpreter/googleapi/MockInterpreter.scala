package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.~>
import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain.{FullCard, GoogleAuthParams, Package}
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle.{Failure}
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.apigoogle._

trait InterpreterServer[F[_]] {
  def getDetails(pack: Package, auth: GoogleAuthParams): F[Failure Xor FullCard]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A]( ops: Ops[A]) = ops match {
    case GetDetails(pack, auth) => server.getDetails(pack, auth)
  }

}

