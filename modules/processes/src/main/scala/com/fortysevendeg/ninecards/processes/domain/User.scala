package com.fortysevendeg.ninecards.processes.domain

case class User(
  sessionToken: String)

case class Installation(
  id: Option[String] = None,
  deviceToken: Option[String] = None,
  userId: Option[String] = None)