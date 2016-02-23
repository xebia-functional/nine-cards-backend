package com.fortysevendeg.ninecards.googleplay.service

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.api.Domain._

object GooglePlayService {
  case class Token(value: String) extends AnyVal
  case class AndroidId(value: String) extends AnyVal
  case class Localization(value: String) extends AnyVal

  type GoogleAuthParams = (Token, AndroidId, Option[Localization])
}
