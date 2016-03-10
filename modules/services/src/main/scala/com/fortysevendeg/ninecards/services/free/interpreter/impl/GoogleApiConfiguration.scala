package com.fortysevendeg.ninecards.services.free.interpreter.impl

case class GoogleApiConfiguration(
  protocol: String,
  host: String,
  port: Option[Int])

object GoogleApiConfiguration {
  implicit def googleApiConfiguration: GoogleApiConfiguration = GoogleApiConfiguration(
    protocol = "https",
    host = "www.googleapis.com",
    port = None
  )
}
