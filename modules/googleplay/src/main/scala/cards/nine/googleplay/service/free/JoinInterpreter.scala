package cards.nine.googleplay.service.free

import cards.nine.googleplay.service.free.algebra.{ Cache, GoogleApi, WebScraper }
import cats.~>

object JoinInterpreter {

  implicit def interpreter[F[_]](implicit
    googleApiInt: GoogleApi.Ops ~> F,
    cacheInt: Cache.Ops ~> F,
    webScrapperInt: WebScraper.Ops ~> F): (JoinServices ~> F) = {
    val int1: (JoinServices1 ~> F) = cacheInt or webScrapperInt
    googleApiInt or int1
  }

}

