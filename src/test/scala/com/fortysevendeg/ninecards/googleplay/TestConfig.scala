package com.fortysevendeg.ninecards.googleplay

import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._

trait TestConfig {
  val token = Token("LgPB-ZyqXGS9YGnl3odfBoAxtEMrcvzi8HeuFVB6NTRJkgAuAZ8ZMj-MX-ZDkJZjct4dCw.")
  val androidId = AndroidId("3D4D7FE45C813D3E")
  val localization = Localization("es-ES")
  val params = (token, androidId, Some(localization))
}

object TestConfig extends TestConfig
