package com.fortysevendeg.ninecards.services.free.domain

case class User(
  id: Option[String] = None,
  email: Option[String] = None,
  sessionToken: String)

case class Installation(
  id: Option[String] = None,
  deviceToken: Option[String] = None,
  userId: Option[String] = None)