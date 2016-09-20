package com.fortysevendeg.ninecards.googleplay.service.free

import cats.~>
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.{apigoogle, cache, webscrapper}

object JoinInterpreter {

  implicit def interpreter[F[_]]( implicit
    googleApiInt: apigoogle.Ops ~> F,
    cacheInt: cache.Ops ~> F,
    webScrapperInt: webscrapper.Ops ~> F
  ): (JoinServices ~> F) = {
    val int_x: (JoinServices_x ~> F) = cacheInt or webScrapperInt
    googleApiInt or int_x
  }

}


