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
package cards.nine.domain.application

import enumeratum.{ Enum, EnumEntry }

sealed trait Category extends EnumEntry
object Category extends Enum[Category] {

  case object ART_AND_DESIGN extends Category
  case object AUTO_AND_VEHICLES extends Category
  case object BEAUTY extends Category
  case object BOOKS_AND_REFERENCE extends Category
  case object BUSINESS extends Category
  case object COMICS extends Category
  case object COMMUNICATION extends Category
  case object DATING extends Category
  case object EDUCATION extends Category
  case object ENTERTAINMENT extends Category
  case object EVENTS extends Category
  case object FINANCE extends Category
  case object FOOD_AND_DRINK extends Category
  case object GAME extends Category
  case object HEALTH_AND_FITNESS extends Category
  case object HOUSE_AND_HOME extends Category
  case object LIBRARIES_AND_DEMO extends Category
  case object LIFESTYLE extends Category
  case object MAPS_AND_NAVIGATION extends Category
  case object MEDICAL extends Category
  case object MUSIC_AND_AUDIO extends Category
  case object NEWS_AND_MAGAZINES extends Category
  case object PARENTING extends Category
  case object PERSONALIZATION extends Category
  case object PHOTOGRAPHY extends Category
  case object PRODUCTIVITY extends Category
  case object SOCIAL extends Category
  case object SHOPPING extends Category
  case object SPORTS extends Category
  case object TOOLS extends Category
  case object TRAVEL_AND_LOCAL extends Category
  case object VIDEO_PLAYERS extends Category
  case object WEATHER extends Category

  case object GAME_ACTION extends Category
  case object GAME_ADVENTURE extends Category
  case object GAME_ARCADE extends Category
  case object GAME_BOARD extends Category
  case object GAME_CARD extends Category
  case object GAME_CASINO extends Category
  case object GAME_CASUAL extends Category
  case object GAME_EDUCATIONAL extends Category
  case object GAME_MUSIC extends Category
  case object GAME_PUZZLE extends Category
  case object GAME_RACING extends Category
  case object GAME_ROLE_PLAYING extends Category
  case object GAME_SIMULATION extends Category
  case object GAME_SPORTS extends Category
  case object GAME_STRATEGY extends Category
  case object GAME_TRIVIA extends Category
  case object GAME_WORD extends Category

  val values = super.findValues

  val valuesName = values.toList map (_.entryName)

  val sortedValues = List(
    COMMUNICATION,
    SOCIAL,
    PRODUCTIVITY,
    PHOTOGRAPHY,
    ENTERTAINMENT,
    GAME,
    VIDEO_PLAYERS,
    MAPS_AND_NAVIGATION,
    MUSIC_AND_AUDIO,
    NEWS_AND_MAGAZINES,
    LIFESTYLE,
    HEALTH_AND_FITNESS,
    SPORTS,
    SHOPPING,
    TRAVEL_AND_LOCAL,
    FINANCE,
    BOOKS_AND_REFERENCE,
    EDUCATION,
    ART_AND_DESIGN,
    FOOD_AND_DRINK,
    AUTO_AND_VEHICLES,
    BEAUTY,
    BUSINESS,
    COMICS,
    DATING,
    EVENTS,
    HOUSE_AND_HOME,
    LIBRARIES_AND_DEMO,
    MEDICAL,
    PARENTING,
    PERSONALIZATION,
    TOOLS,
    WEATHER
  ) map (_.entryName)
}
