package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.processes.{UserProcesses, NineCardsServices, AppProcesses}
import NineCardsServices._

object DependencyInjector {

  lazy val appProcesses = new AppProcesses[NineCardsServices]()

  lazy val userProcess = new UserProcesses[NineCardsServices]()
}
