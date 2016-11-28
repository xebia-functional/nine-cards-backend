package cards.nine.googleplay.processes

import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cats.data.Coproduct
import cats.~>

object GooglePlayApp {

  type GooglePlayAppC01[A] = Coproduct[GoogleApi.Ops, WebScraper.Ops, A]
  type GooglePlayApp[A] = Coproduct[Cache.Ops, GooglePlayAppC01, A]

  object Interpreters {
    def apply[F[_]](
      googleApiInt: GoogleApi.Ops ~> F,
      cacheInt: Cache.Ops ~> F,
      webScrapperInt: WebScraper.Ops ~> F
    ): (GooglePlayApp ~> F) = {
      val interpreters01: (GooglePlayAppC01 ~> F) = googleApiInt or webScrapperInt
      cacheInt or interpreters01
    }
  }
}
