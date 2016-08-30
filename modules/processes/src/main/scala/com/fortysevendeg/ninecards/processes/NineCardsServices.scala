package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.{ Monad, ~> }
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra.{ GoogleAnalytics, GoogleApi, GooglePlay }
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  implicit val taskMonadInstance: Monad[Task] = taskMonad

  type NineCardsServicesC01[A] = Coproduct[GooglePlay.Ops, GoogleApi.Ops, A]
  type NineCardsServicesC02[A] = Coproduct[GoogleAnalytics.Ops, NineCardsServicesC01, A]
  type NineCardsServices[A] = Coproduct[DBResult, NineCardsServicesC02, A]

  val prodInterpretersC01: NineCardsServicesC01 ~> Task =
    taskInterpreters.googlePlayInterpreter or taskInterpreters.googleApiInterpreter

  val prodInterpretersC02: NineCardsServicesC02 ~> Task =
    taskInterpreters.analyticsInterpreter or prodInterpretersC01

  val prodInterpreters: NineCardsServices ~> Task =
    taskInterpreters.dBResultInterpreter or prodInterpretersC02
}