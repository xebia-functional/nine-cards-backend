package cards.nine.processes.applications

import cards.nine.domain.application.{ CardList, FullCard }
import org.scalacheck.Shapeless._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class ConvertersSpec
  extends Specification
  with ScalaCheck {

  import Converters._

  "filterCategorized" should {
    "convert an AppsInfo to a GetAppsInfoResponse object" in {
      prop { appsInfo: CardList[FullCard] ⇒

        val appsWithoutCategories = appsInfo.cards.filter(app ⇒ app.categories.isEmpty)

        val categorizeAppsResponse = filterCategorized(appsInfo)

        categorizeAppsResponse.missing shouldEqual appsInfo.missing ++ appsWithoutCategories.map(_.packageName)

        forall(categorizeAppsResponse.cards) { item ⇒
          appsInfo.cards.exists { app ⇒
            app.packageName == item.packageName &&
              app.categories == item.categories &&
              app.categories.nonEmpty
          } should beTrue
        }
      }
    }
  }

}
