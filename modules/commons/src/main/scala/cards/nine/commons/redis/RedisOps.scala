package cards.nine.commons.redis

import akka.actor.ActorSystem
import cards.nine.commons.catscalaz.ScalaFuture2Task
import cards.nine.commons.config.Domain.RedisConfiguration
import cats.{ Applicative, ~> }
import cats.data.Kleisli
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.concurrent.Task
import scredis.{ Client ⇒ ScredisClient }

object RedisOps {

  implicit val applicative: Applicative[RedisOps] =
    Applicative[Kleisli[Task, RedisClient, ?]]

  def withRedisClient[A](f: RedisClient ⇒ Future[A])(implicit ec: ExecutionContext): RedisOps[A] =
    Kleisli(client ⇒ ScalaFuture2Task(f(client)))

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

