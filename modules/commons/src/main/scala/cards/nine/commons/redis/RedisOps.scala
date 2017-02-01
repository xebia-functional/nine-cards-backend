/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.commons.redis

import akka.actor.ActorSystem
import cards.nine.commons.catscalaz.TaskInstances
import cards.nine.commons.config.Domain.RedisConfiguration
import cats.{ Applicative, ~> }
import scalaz.concurrent.Task
import scredis.{ Client ⇒ ScredisClient }

object RedisOps {

  import TaskInstances.taskMonad

  object RedisOpInstances extends Applicative[RedisOps] {

    override def pure[A](x: A): RedisOps[A] =
      _client ⇒ taskMonad.pure(x)

    override def ap[A, B](ff: RedisOps[A ⇒ B])(fa: RedisOps[A]): RedisOps[B] =
      client ⇒ {
        val lhs = ff(client)
        val rhs = fa(client)
        taskMonad.ap(lhs)(rhs)
      }

  }

  implicit val applicative: Applicative[RedisOps] = RedisOpInstances

}

class RedisOpsToTask(redis: ScredisClient) extends (RedisOps ~> Task) {

  override def apply[A](fa: RedisOps[A]): Task[A] =
    redis.withTransaction[Task[A]](build ⇒ fa(build))
}

object RedisOpsToTask {

  def apply(config: RedisConfiguration)(implicit actorSystem: ActorSystem): RedisOpsToTask = {
    val client: ScredisClient = ScredisClient(
      host        = config.host,
      port        = config.port,
      passwordOpt = config.secret
    )
    new RedisOpsToTask(client)
  }

}

