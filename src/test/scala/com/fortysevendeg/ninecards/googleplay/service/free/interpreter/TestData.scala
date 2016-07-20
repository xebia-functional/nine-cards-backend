package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Package
import com.fortysevendeg.ninecards.config.NineCardsConfig

object TestData {

  def apiEndpoint = NineCardsConfig.getConfigValue("googleplay.api.endpoint")
  def webEndpoint = NineCardsConfig.getConfigValue("googleplay.web.endpoint")

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)
    val title = "Shapes & Colors Music Show"
    val categories = List("EDUCATION", "FAMILY_EDUCATION")

    lazy val protobufFile = getClass.getClassLoader.getResource(packageName)
    lazy val htmlFile     = getClass.getClassLoader.getResource(packageName + ".html")

  }

  val nonexisting = "com.package.does.not.exist"

}

