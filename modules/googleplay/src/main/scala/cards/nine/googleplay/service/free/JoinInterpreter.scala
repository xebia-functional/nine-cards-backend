package cards.nine.googleplay.service.free

import cats.~>
import cards.nine.googleplay.service.free.algebra.{ apigoogle, cache, webscrapper }

object JoinInterpreter {

  implicit def interpreter[F[_]](implicit
    googleApiInt: apigoogle.Ops ~> F,
    cacheInt: cache.Ops ~> F,
    webScrapperInt: webscrapper.Ops ~> F): (JoinServices ~> F) = {
    val int1: (JoinServices1 ~> F) = cacheInt or webScrapperInt
    googleApiInt or int1
  }

}

