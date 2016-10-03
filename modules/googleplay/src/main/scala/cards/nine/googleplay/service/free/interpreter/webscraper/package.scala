package cards.nine.googleplay.service.free.interpreter

import org.http4s.client.Client
import scalaz.concurrent.Task

package object webscrapper {

  type WithClient[+A] = Client ⇒ Task[A]

}