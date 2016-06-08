package com.fortysevendeg.ninecards.googleplay.service.free

import cats.data.Xor
import scalaz.concurrent.Task
import com.fortysevendeg.ninecards.googleplay.domain.Domain.{Item, Package}
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain.GoogleAuthParams

package object interpreter {

  type QueryRequest = (Package, GoogleAuthParams)
  type QueryResult  = Xor[String, Item]
  type QueryService = QueryRequest => Task[QueryResult]

}
