package com.fortysevendeg.ninecards.processes

import cats._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

trait TestInterpreters {

  val testInterpretersC01: NineCardsServicesC01 ~> Id =
    idInterpreters.googlePlayInterpreter or idInterpreters.googleApiInterpreter

  val testInterpreters: NineCardsServices ~> Id =
    idInterpreters.dBResultInterpreter or testInterpretersC01

}
