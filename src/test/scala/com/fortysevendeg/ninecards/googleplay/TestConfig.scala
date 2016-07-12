package com.fortysevendeg.ninecards.googleplay

trait TestConfig {

  import com.fortysevendeg.ninecards.config.NineCardsConfig.getConfigValue
  import com.fortysevendeg.ninecards.googleplay.domain._

  lazy val token = Token( getConfigValue("test.token") )
  lazy val androidId = AndroidId( getConfigValue("test.androidId") )
  lazy val localization = Localization( getConfigValue("test.localization") )

  lazy val authParams = GoogleAuthParams(androidId, token, Some(localization))
}

object TestConfig extends TestConfig
