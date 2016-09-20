package com.fortysevendeg.ninecards.googleplay.service

import cats.data.Coproduct
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.{apigoogle, cache, webscrapper}

package object free {

  type JoinServices_x[A] = Coproduct[cache.Ops, webscrapper.Ops, A]
  type JoinServices[A] = Coproduct[apigoogle.Ops, JoinServices_x, A]

}


