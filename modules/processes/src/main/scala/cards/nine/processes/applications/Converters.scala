package cards.nine.processes.applications

import cards.nine.domain.analytics._
import cards.nine.domain.application.{ CardList, FullCard, Moment, Package }

private[applications] object Converters {

  def filterCategorized(info: CardList[FullCard]): CardList[FullCard] = {
    val (appsWithoutCategories, apps) = info.cards.partition(app ⇒ app.categories.isEmpty)
    CardList(
      missing = info.missing ++ appsWithoutCategories.map(_.packageName),
      cards   = apps
    )
  }

  def toUnrankedApp(category: String)(pack: Package) = UnrankedApp(pack, category)

  def toMoment(widgetMoment: String) = widgetMoment.replace(Moment.widgetMomentPrefix, "")

  def toWidgetMoment(moment: String) = s"${Moment.widgetMomentPrefix}$moment"

  def toRankedAppsByCategory(limit: Option[Int] = None)(ranking: (String, List[RankedApp])) = {
    val (category, apps) = ranking

    RankedAppsByCategory(category, limit.fold(apps)(apps.take))
  }

  def toRankedWidgetsByMoment(limit: Int)(ranking: (String, List[RankedWidget])) = {
    val (moment, widgets) = ranking

    RankedWidgetsByMoment(toMoment(moment), widgets.take(limit))
  }
}