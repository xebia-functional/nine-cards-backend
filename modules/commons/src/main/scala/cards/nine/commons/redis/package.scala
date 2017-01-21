package cards.nine.commons

import cats.data.Kleisli
import scredis.commands.{ KeyCommands, ListCommands, ScriptingCommands, SetCommands, StringCommands }
import scalaz.concurrent.Task

package object redis {

  type RedisClient = KeyCommands with ListCommands with ScriptingCommands with SetCommands with StringCommands

  type RedisOps[A] = Kleisli[Task, RedisClient, A]

}
