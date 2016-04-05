package com.fortysevendeg.ninecards.services.free.interpreter

import cats.~>
import com.fortysevendeg.ninecards.services.free.algebra.DBResult._
import com.fortysevendeg.ninecards.services.free.algebra.GoogleApiServices._
import com.fortysevendeg.ninecards.services.free.interpreter.impl.GoogleApiServices._

import scalaz.concurrent.Task

object Interpreters {

  object DBResultInterpreter extends (DBResult ~> Task) {
    def apply[A](fa: DBResult[A]) = fa match {
      case DBSuccess(value) ⇒ Task.now(value)
      case DBFailure(e)     ⇒ Task.fail(e)
    }
  }

  object GoogleAPIServicesInterpreter extends (GoogleApiOps ~> Task) {

    def apply[A](fa: GoogleApiOps[A]) = fa match {
      case GetTokenInfo(tokenId: String) ⇒
        googleApiServices.getTokenInfo(tokenId)
    }
  }

}
