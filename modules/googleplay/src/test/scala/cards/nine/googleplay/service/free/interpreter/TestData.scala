/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.googleplay.service.free.interpreter

import cards.nine.commons.config.NineCardsConfig._
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.domain._

object TestData {

  def webEndpoint = nineCardsConfiguration.test.googlePlayDetailsUrl

  object nonexisting {
    val packageName = "com.package.does.not.exist"
    val packageObj = Package(packageName)
    val infoError = InfoError(packageName)
  }

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    val card = FullCard(
      packageName = packageObj,
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
      packageName = packageObj,
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
      packageName = packageObj,
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

