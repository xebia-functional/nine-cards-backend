package com.fortysevendeg.ninecards.services.free.domain

case class TokenInfo(
  iss:            String,
  at_hash:        String,
  aud:            String,
  sub:            String,
  email_verified: String,
  azp:            String,
  hd:             Option[String],
  email:          String,
  iat:            String,
  exp:            String,
  alg:            String,
  kid:            String
)

case class WrongTokenInfo(error_description: String)