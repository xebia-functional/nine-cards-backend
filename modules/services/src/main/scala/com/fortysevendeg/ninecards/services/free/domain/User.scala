package com.fortysevendeg.ninecards.services.free.domain

case class User(
  id: Option[String] = None,
  email: Option[String] = None,
  sessionToken: Option[String] = None)

case class Installation(
  id: Long,
  userId: Long,
  deviceToken: Option[String] = None,
  androidId: String)