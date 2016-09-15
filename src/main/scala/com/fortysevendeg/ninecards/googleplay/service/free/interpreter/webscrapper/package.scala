package  com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import org.http4s.client.Client

package object webscrapper {

  type WithClient[+A] = Client => A
}