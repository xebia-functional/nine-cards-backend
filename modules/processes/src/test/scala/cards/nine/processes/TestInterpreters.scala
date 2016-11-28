package cards.nine.processes

import cards.nine.processes.NineCardsServices._
import cats._

import scalaz.concurrent.Task

trait TestInterpreters {

  val task2Id: (Task ~> Id) = new (Task ~> Id) {
    override def apply[A](fa: Task[A]): Id[A] = fa.unsafePerformSync
  }

  val testInterpreters: NineCardsServices ~> Id =
    new NineCardsInterpreters().interpreters.andThen(task2Id)
}
