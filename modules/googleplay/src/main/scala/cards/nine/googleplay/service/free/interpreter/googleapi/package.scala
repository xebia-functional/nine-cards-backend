package cards.nine.googleplay.service.free.interpreter

import org.http4s.client.Client
import scalaz.concurrent.Task

package object googleapi {

  type WithHttpClient[+A] = Client => Task[A]

}
