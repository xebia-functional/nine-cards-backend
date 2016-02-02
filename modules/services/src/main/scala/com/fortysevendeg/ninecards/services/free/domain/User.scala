package com.fortysevendeg.ninecards.services.free.domain

case class User(
  id: Long,
  email: String,
  sessionToken: String,
  banned: Boolean)

case class Installation(
  id: Option[String] = None,
  deviceToken: Option[String] = None,
  androidId: String)

case class Device(
  id: Long,
  userId: Long,
  deviceToken: Option[String] = None,
  androidId: String)