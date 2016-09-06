package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.{ Monad, ~> }
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra._
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  implicit val taskMonadInstance: Monad[Task] = taskMonad

  type NineCardsServicesC03[A] = Coproduct[GooglePlay.Ops, GoogleApi.Ops, A]
  type NineCardsServicesC02[A] = Coproduct[GoogleAnalytics.Ops, NineCardsServicesC03, A]
  type NineCardsServicesC01[A] = Coproduct[Firebase.Ops, NineCardsServicesC02, A]
  type NineCardsServices[A] = Coproduct[DBResult, NineCardsServicesC01, A]

  val prodInterpretersC03: NineCardsServicesC03 ~> Task =
    taskInterpreters.googlePlayInterpreter or taskInterpreters.googleApiInterpreter

  val prodInterpretersC02: NineCardsServicesC02 ~> Task =
    taskInterpreters.analyticsInterpreter or prodInterpretersC03

  val prodInterpretersC01: NineCardsServicesC01 ~> Task =
    taskInterpreters.firebaseInterpreter or prodInterpretersC02

  val prodInterpreters: NineCardsServices ~> Task =
    taskInterpreters.dBResultInterpreter or prodInterpretersC01
}