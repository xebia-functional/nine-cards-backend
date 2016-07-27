package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.{ Monad, ~> }
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.{ GoogleApi, GooglePlay }
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  type NineCardsServicesC01[A] = Coproduct[GooglePlay.Ops, GoogleApi.Ops, A]
  type NineCardsServices[A] = Coproduct[DBResult, NineCardsServicesC01, A]

  implicit val taskMonadInstance: Monad[Task] = taskMonad

  val prodInterpretersC01: NineCardsServicesC01 ~> Task =
    taskInterpreters.googlePlayInterpreter or taskInterpreters.googleApiInterpreter

  val prodInterpreters: NineCardsServices ~> Task =
    taskInterpreters.dBResultInterpreter or prodInterpretersC01
}