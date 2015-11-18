package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.{UserProcesses, AppProcesses}
import com.fortysevendeg.ninecards.NineCardsServices._

object DependencyInjector {

  lazy val appProcesses = new AppProcesses[NineCardsServices]()

  lazy val userProcess = new UserProcesses[NineCardsServices]()
}
