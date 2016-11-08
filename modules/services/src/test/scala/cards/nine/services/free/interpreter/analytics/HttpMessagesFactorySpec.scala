package cards.nine.services.free.interpreter.analytics

import cards.nine.domain.analytics.DateRange
import cards.nine.domain.ScalaCheck._
import cards.nine.services.free.interpreter.analytics.HttpMessagesFactory.CountriesWithRankingReport
import cards.nine.services.free.interpreter.analytics.model.RequestBody
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class HttpMessagesFactorySpec extends Specification with ScalaCheck {

  "CountriesWithRankingReport.buildRequest" should {
    "create a valid RequestBody for the CountriesWithRankingReport" in {
      prop { (dateRange: DateRange, viewId: String) ⇒
        CountriesWithRankingReport.buildRequest(dateRange, viewId) must beLike[RequestBody] {
          case requestBody: RequestBody ⇒
            requestBody.reportRequests must haveSize(1)
        }
      }
    }
  }
}
