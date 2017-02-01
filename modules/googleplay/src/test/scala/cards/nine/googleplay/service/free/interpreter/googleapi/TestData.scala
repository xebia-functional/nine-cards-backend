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
package cards.nine.googleplay.service.free.interpreter.googleapi

import cards.nine.domain.application.{ FullCard, Package }

object TestData {

  object fisherPrice {
    val packageName = "air.fisherprice.com.shapesAndColors"
    val packageObj = Package(packageName)

    lazy val protobufFile = getClass.getClassLoader.getResource(packageName)
    lazy val htmlFile = getClass.getClassLoader.getResource(packageName + ".html")

    val card = FullCard(
      packageName = packageObj,
      title       = "Shapes & Colors Music Show",
      free        = true,
      icon        = "http://lh4.ggpht.com/Pb8iLNmi9vHOwB-39TKe-kn4b_uU-E6rn7zSiFz6jC0RlaEQeNCcBh2MueyslcQ3mj2H",
      stars       = 4.070538520812988,
      downloads   = "1,000,000+",
      screenshots = List(
        "http://lh4.ggpht.com/fi-LxRsm8E5-940Zc5exQQyb4WWt1Q9D4oQFfEMP9oX0sWgV2MmIVAKwjtMN7ns5k7M",
        "http://lh3.ggpht.com/3ojygv7ZArhODcEq_JTaYx8ap4WwrgU6qYzspYyuEH24byhtqsgSaS0W9YN6A8ySSXA",
        "http://lh4.ggpht.com/974sdpZY4MiXIDn4Yyutylbh7cecJ7nKhHUz3LA3fAR3HdPwyM3yFUOdmcSlCwWjJiYc"
      ),
      categories  = List("EDUCATION")
    )
  }

  object searchCosmos {

    val queryWord = "cosmos"

    val fileName = "search/cosmos"

    val results = List(
      "com.support.cosmos",
      "com.wallpapers4k.cosmos",
      "interstellar.flight",
      "com.cosmostory.cosmosstory",
      "com.nosixfive.verto",
      "com.cosmos.app",
      "com.cebicdroid.cosmolast",
      "com.itg.cosmopolitan",
      "com.google.android.apps.docs",
      "com.AbsintheGames.IC",
      "com.appmajik.cosmo",
      "com.themes.studio.cosmos.theme",
      "ca.surrey.cosmos",
      "com.turbochilli.rollingsky",
      "com.umiak.universefree",
      "spiralcl.journey",
      "com.external.cosmos",
      "com.noctuasoftware.retrocosmos2",
      "hr.artplus.homagecosmos.app",
      "com.cosmos.paw.patrol.slasher.free"
    ) map Package

  }

}