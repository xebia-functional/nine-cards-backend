package com.fortysevendeg.ninecards.googleplay.service.free

import cats.data.Xor
import scalaz.concurrent.Task
import com.fortysevendeg.ninecards.googleplay.domain.{AppRequest, Item}

package object interpreter {

  type AppService = AppRequest => Task[Xor[String, Item]]

}
