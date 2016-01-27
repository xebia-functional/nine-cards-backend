package com.fortysevendeg.ninecards.services.free.algebra

import cats.free.{Free, Inject}

import scala.language.higherKinds

object DBResult {

  sealed trait DBResult[A]

  case class DBSuccess[A](a: A) extends DBResult[A]

  case class DBFailure[A](e: Throwable) extends DBResult[A]

  class DBOps[F[_]](implicit I: Inject[DBResult, F]) {

    def success[A](value: A): Free[F, A] = Free.inject[DBResult, F](DBSuccess(value))

    def failure[A](e: Throwable): Free[F, A] = Free.inject[DBResult, F](DBFailure(e))

  }

  object DBOps {

    implicit def dbOps[F[_]](implicit I: Inject[DBResult, F]): DBOps[F] = new DBOps

  }

}
