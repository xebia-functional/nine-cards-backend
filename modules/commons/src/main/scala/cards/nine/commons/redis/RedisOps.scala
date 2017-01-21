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

