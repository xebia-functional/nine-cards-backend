package cards.nine.googleplay.service.free.interpreter

import cards.nine.commons.NineCardsConfig._
import cards.nine.googleplay.domain._

object TestData {

  def googleApiConf = googleapi.Configuration.load()
  def webEndpoint = defaultConfig.getString("ninecards.googleplay.web.endpoint")

  object nonexisting {
    val packageName = "com.package.does.not.exist"
    val packageObj = Package(packageName)
    val infoError = InfoError(packageName)
  }

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageName,
      title       = "Shapes & Colors Music Show",
      free        = true,
      icon        = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H",
      stars       = 4.071812152862549,
      downloads   = "1.000.000 - 5.000.000",
      screenshots = List(),
      categories  = List("EDUCATION", "FAMILY_EDUCATION")
    )
  }

  object minecraft {
    val packageName = "com.mojang.minecraftpe"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageName,
      title       = "Minecraft: Pocket Edition",
      free        = false,
      icon        = "http://lh3.googleusercontent.com/30koN0eGl-LHqvUZrCj9HT4qVPQdvN508p2wuhaWUnqKeCp6nrs9QW8v6IVGvGNauA",
      stars       = 4.4701409339904785,
      downloads   = "10,000,000+",
      screenshots = List(),
      categories  = List("GAME_ARCADE")
    )

  }

  object fortyseven {
    val packageName = "com.fortyseven.deg"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageName,
      title       = "Forty Seven Degrees",
      free        = false,
      icon        = "http://icon",
      stars       = 4.214,
      downloads   = "455 - 2001",
      screenshots = List(),
      categories  = List("Consultancy")
    )
  }

}

