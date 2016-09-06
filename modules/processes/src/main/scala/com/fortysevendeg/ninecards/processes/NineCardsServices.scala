package com.fortysevendeg.ninecards.processes

import cats.data.Coproduct
import cats.{ Monad, ~> }
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBResult
import com.fortysevendeg.ninecards.services.free.algebra._
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

import scalaz.concurrent.Task

object NineCardsServices {

  implicit val taskMonadInstance: Monad[Task] = taskMonad

  type NineCardsServicesC03[A] = Coproduct[GooglePlay.Ops, GoogleApi.Ops, A]
  type NineCardsServicesC02[A] = Coproduct[GoogleAnalytics.Ops, NineCardsServicesC03, A]
  type NineCardsServicesC01[A] = Coproduct[Firebase.Ops, NineCardsServicesC02, A]
  type NineCardsServices[A] = Coproduct[DBResult, NineCardsServicesC01, A]

  class NineCardsInterpreters[F[_]](int: Interpreters[F]) {

    val interpretersC03: NineCardsServicesC03 ~> F = int.googlePlayInterpreter or int.googleApiInterpreter

    val interpretersC02: NineCardsServicesC02 ~> F = int.analyticsInterpreter or interpretersC03

    val interpretersC01: NineCardsServicesC01 ~> F = int.firebaseInterpreter or interpretersC02

    val interpreters: NineCardsServices ~> F = int.dBResultInterpreter or interpretersC01
  }

  val prodNineCardsInterpreters = new NineCardsInterpreters(taskInterpreters)

  val prodInterpreters: NineCardsServices ~> Task = prodNineCardsInterpreters.interpreters
}