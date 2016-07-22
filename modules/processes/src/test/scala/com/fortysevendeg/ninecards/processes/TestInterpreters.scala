package com.fortysevendeg.ninecards.processes

import cats._
import com.fortysevendeg.ninecards.processes.NineCardsServices.NineCardsServices
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

trait TestInterpreters {

  val testInterpreters: NineCardsServices ~> Id =
    idInterpreters.dBResultInterpreter or idInterpreters.googleApiInterpreter

}
