package com.fortysevendeg.ninecards.googleplay.service

import cats.data.Coproduct
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.{apigoogle, cache, webscrapper}

package object free {

  type JoinServices1[A] = Coproduct[cache.Ops, webscrapper.Ops, A]
  type JoinServices[A] = Coproduct[apigoogle.Ops, JoinServices1, A]

}


