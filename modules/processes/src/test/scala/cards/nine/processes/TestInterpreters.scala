package cards.nine.processes

import cats._
import cards.nine.processes.NineCardsServices._
import cards.nine.services.free.interpreter.Interpreters._

trait TestInterpreters {

  val testNineCardsInterpreters = new NineCardsInterpreters[Id](idInterpreters)

  val testInterpreters: NineCardsServices ~> Id = testNineCardsInterpreters.interpreters
}
