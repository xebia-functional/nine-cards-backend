package com.fortysevendeg.ninecards.services.free.domain

import enumeratum.{ EnumEntry, Enum }

case class PackageName(name: String) extends AnyVal

sealed trait Category extends EnumEntry
object Category extends Enum[Category] {

  case object ALL_APPS extends Category
  case object ALL_CATEGORIES extends Category
  case object CUSTOM extends Category
  case object MISC extends Category
  case object GAME extends Category
  case object BOOKS_AND_REFERENCE extends Category
  case object BUSINESS extends Category
  case object COMICS extends Category
  case object COMMUNICATION extends Category
  case object EDUCATION extends Category
  case object ENTERTAINMENT extends Category
  case object FINANCE extends Category
  case object HEALTH_AND_FITNESS extends Category
  case object LIBRARIES_AND_DEMO extends Category
  case object LIFESTYLE extends Category
  case object APP_WALLPAPER extends Category
  case object MEDIA_AND_VIDEO extends Category
  case object MEDICAL extends Category
  case object MUSIC_AND_AUDIO extends Category
  case object NEWS_AND_MAGAZINES extends Category
  case object PERSONALIZATION extends Category
  case object PHOTOGRAPHY extends Category
  case object PRODUCTIVITY extends Category
  case object SHOPPING extends Category
  case object SOCIAL extends Category
  case object SPORTS extends Category
  case object TOOLS extends Category
  case object TRANSPORTATION extends Category
  case object TRAVEL_AND_LOCAL extends Category
  case object WEATHER extends Category
  case object APP_WIDGETS extends Category
  case object CONTACTS extends Category

  case object GAME_ACTION extends Category
  case object GAME_ADVENTURE extends Category
  case object GAME_RACING extends Category
  case object GAME_CARD extends Category
  case object GAME_CASINO extends Category
  case object GAME_CASUAL extends Category
  case object GAME_FAMILY extends Category
  case object GAME_SPORTS extends Category
  case object GAME_EDUCATIONAL extends Category
  case object GAME_STRATEGY extends Category
  case object GAME_WALLPAPER extends Category
  case object GAME_TRIVIA extends Category
  case object GAME_BOARD extends Category
  case object GAME_ROLE_PLAYING extends Category
  case object GAME_MUSIC extends Category
  case object GAME_WORD extends Category
  case object GAME_PUZZLE extends Category
  case object GAME_ARCADE extends Category
  case object GAME_SIMULATION extends Category
  case object GAME_WIDGETS extends Category

  /* These Are NineCards MOMENTS, not CategorieS*/
  case object HOME extends Category
  case object WORK extends Category
  case object NIGHT extends Category
  case object TRANSIT extends Category

  val values = super.findValues
}

