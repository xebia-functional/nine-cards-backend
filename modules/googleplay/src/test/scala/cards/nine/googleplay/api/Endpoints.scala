package cards.nine.googleplay.api

object endpoints {
  val item = "GET '/googleplay/package/{pkg}, the endpoint to get an Item,"
  val itemList = "POST '/googleplay/packages/detailed', the endpoint to get several app's items,"
  val card = "GET '/googleplay/cards/{pkg}, the endpoint to get the card of one app,"
  val cardList = "POST '/googleplay/cards, the endpoint to get the cards of several apps, "
  val recommendCategory = "GET '/googleplay/recommendations/{category}/{filter}', the endpoint to get recommendations by a category,"
  val recommendAppList = "POST '/googleplay/recommendations', the endpoint to get recommendations for a list of apps,"
}

