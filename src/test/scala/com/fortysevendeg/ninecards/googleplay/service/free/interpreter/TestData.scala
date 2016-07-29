package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.config.NineCardsConfig

object TestData {

  def apiEndpoint = NineCardsConfig.getConfigValue("googleplay.api.endpoint")
  def webEndpoint = NineCardsConfig.getConfigValue("googleplay.web.endpoint")

  object nonexisting {
    val packageName = "com.package.does.not.exist"
    val packageObj = Package(packageName)
    val infoError = InfoError(packageName)
  }

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)
    val title = "Shapes & Colors Music Show"
    val categories = List("EDUCATION", "FAMILY_EDUCATION")
    val icon = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H"
    val stars = 4.10

    lazy val protobufFile = getClass.getClassLoader.getResource(packageName)
    lazy val htmlFile     = getClass.getClassLoader.getResource(packageName + ".html")

    val card = AppCard(
      packageName = packageName,
      title = title,
      free = true,
      icon = icon,
      stars = stars,
      downloads = "",
      categories = categories
    )
  }

}

