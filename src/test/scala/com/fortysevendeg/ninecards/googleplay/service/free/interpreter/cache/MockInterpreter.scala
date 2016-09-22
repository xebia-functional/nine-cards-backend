package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.cache

import cats.~>
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.cache._
import org.joda.time.DateTime

trait InterpreterServer[F[_]] {
  def getValid( pack: Package): F[Option[FullCard]]
  def putResolved(card: FullCard) :  F[Unit]
  def markPending(pack: Package): F[Unit]
  def unmarkPending(pack: Package): F[Unit]
  def markError(pack: Package, date: DateTime): F[Unit]
  def clearInvalid(pack: Package) : F[Unit]
  def isPending(pack: Package) : F[Boolean]
  def listPending(num: Int) : F[List[Package]]
}

case class MockInterpreter[F[_]](server: InterpreterServer[F]) extends (Ops ~> F) {

  override def apply[A]( ops: Ops[A]) = ops match {
    case GetValid(pack) => server.getValid(pack)
    case PutResolved(card) => server.putResolved(card)
    case MarkPending(pack) => server.markPending(pack)
    case UnmarkPending(pack) => server.unmarkPending(pack)
    case MarkError(pack, date) => server.markError(pack,date)
    case ClearInvalid(pack) => server.clearInvalid(pack)
    case IsPending( pack) => server.isPending( pack)
    case ListPending(num) => server.listPending(num)
  }

}
