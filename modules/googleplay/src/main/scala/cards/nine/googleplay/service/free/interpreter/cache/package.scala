package cards.nine.googleplay.service.free.interpreter

import com.redis.RedisClient
import scalaz.concurrent.Task

package object cache {

  type WithRedisClient[+A] = RedisClient ⇒ Task[A]

}