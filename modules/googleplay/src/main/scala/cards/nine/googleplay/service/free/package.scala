package cards.nine.googleplay.service

import cats.data.Coproduct
import cards.nine.googleplay.service.free.algebra.{ GoogleApi, Cache, WebScraper }

package object free {

  type JoinServices1[A] = Coproduct[Cache.Ops, WebScraper.Ops, A]
  type JoinServices[A] = Coproduct[GoogleApi.Ops, JoinServices1, A]

}

