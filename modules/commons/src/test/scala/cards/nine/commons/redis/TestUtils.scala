package cards.nine.commons.redis

import akka.actor.ActorSystem

object TestUtils {

  implicit val redisTestActorSystem: ActorSystem = ActorSystem("cards-nine-test-commons-redis-tests")

}
