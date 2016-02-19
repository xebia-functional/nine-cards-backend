package com.fortysevendeg.ninecards.googleplay.service

import org.http4s.Http4s._
import scalaz.concurrent.Task
import GooglePlayService._
import com.fortysevendeg.ninecards.api.Domain._

object Http4sGooglePlayService {

  def packageRequest(params: GoogleAuthParams): Package => Task[Option[Item]] = ???
}
