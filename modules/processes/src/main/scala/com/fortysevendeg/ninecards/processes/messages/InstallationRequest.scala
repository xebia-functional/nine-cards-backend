package com.fortysevendeg.ninecards.processes.messages

case class InstallationRequest(
  id: Option[String] = None,
  deviceType: String,
  deviceToken: Option[String] = None,
  userId: Option[String] = None,
  channels: Option[List[String]] = None)