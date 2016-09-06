package com.fortysevendeg.ninecards.processes

import cats._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

trait TestInterpreters {

  val testNineCardsInterpreters = new NineCardsInterpreters[Id](idInterpreters)

  val testInterpreters: NineCardsServices ~> Id = testNineCardsInterpreters.interpreters
}
