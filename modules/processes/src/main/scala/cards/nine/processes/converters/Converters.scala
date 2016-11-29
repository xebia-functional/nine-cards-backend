package cards.nine.processes.converters

import java.sql.Timestamp

import cards.nine.domain.analytics._
import cards.nine.domain.application.{ CardList, FullCard, Moment, Package }
import cards.nine.processes.messages.InstallationsMessages._
import cards.nine.processes.messages.UserMessages.LoginResponse
import cards.nine.services.free.domain.{ Installation ⇒ InstallationServices, User ⇒ UserAppServices }
import org.joda.time.DateTime

object Converters {

  implicit def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  implicit def toTimestamp(datetime: DateTime): Timestamp = new Timestamp(datetime.getMillis)

  def toLoginResponse(info: (UserAppServices, InstallationServices)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      apiKey       = user.apiKey,
      sessionToken = user.sessionToken
    )
  }

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId   = installation.androidId,
      deviceToken = installation.deviceToken
    )

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
