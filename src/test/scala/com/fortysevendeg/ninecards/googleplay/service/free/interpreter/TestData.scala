package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.config.NineCardsConfig

object TestData {

  def apiEndpoint = NineCardsConfig.getConfigValue("ninecards.googleplay.api.endpoint")
  def webEndpoint = NineCardsConfig.getConfigValue("ninecards.googleplay.web.endpoint")

  object nonexisting {
    val packageName = "com.package.does.not.exist"
    val packageObj = Package(packageName)
    val infoError = InfoError(packageName)
  }

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    lazy val protobufFile = getClass.getClassLoader.getResource(packageName)
    lazy val htmlFile     = getClass.getClassLoader.getResource(packageName + ".html")

    val card = AppCard(
      packageName = packageName,
      title = "Shapes & Colors Music Show",
      free = true,
      icon = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H",
      stars = 4.071812152862549,
      downloads = "1.000.000 - 5.000.000",
      categories = List("EDUCATION", "FAMILY_EDUCATION")
    )
  }

  object minecraft {
    val packageName = "com.mojang.minecraftpe"
    val packageObj = Package(packageName)

    val card = AppCard(
      packageName = packageName,
      title = "Minecraft: Pocket Edition",
      free = false,
      icon = "http://lh3.googleusercontent.com/30koN0eGl-LHqvUZrCj9HT4qVPQdvN508p2wuhaWUnqKeCp6nrs9QW8v6IVGvGNauA",
      stars = 4.4701409339904785,
      downloads = "10,000,000+",
      categories = List("GAME_ARCADE")
    )

  }

}

