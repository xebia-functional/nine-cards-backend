package com.fortysevendeg.ninecards.processes.messages

case class InstallationRequest(
  deviceToken: Option[String] = None,
  userId: Option[String] = None)