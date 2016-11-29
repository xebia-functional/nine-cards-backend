package cards.nine.commons

import scredis.commands.{ KeyCommands, ListCommands, ScriptingCommands, StringCommands }
import scalaz.concurrent.Task

package object redis {

  type RedisClient = KeyCommands with ListCommands with ScriptingCommands with StringCommands

  type RedisOps[+A] = RedisClient ⇒ Task[A]

}
