package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.messages.{ rankings ⇒ Api }
import com.fortysevendeg.ninecards.processes.messages.{ rankings ⇒ Proc }
import com.fortysevendeg.ninecards.services.free.domain.Category
import com.fortysevendeg.ninecards.services.free.domain.{ rankings ⇒ Domain }
import com.fortysevendeg.ninecards.services.common.NineCardsConfig._
import org.joda.time.DateTime

object rankings {

  import Domain.{ AuthParams, DateRange, RankingParams }

  def toApiRanking(resp: Proc.Get.Response): Api.Ranking = {

    def toApiCatRanking(cat: Category, rank: Domain.CategoryRanking): Api.CategoryRanking =
      Api.CategoryRanking(cat, rank.ranking map (_.name))

    Api.Ranking(resp.ranking.categories.toList map ((toApiCatRanking _).tupled))
  }

  object reload {

    def toRankingParams(token: String, request: Api.Reload.Request): RankingParams = {
      val length = request.rankingLength
      // val dateRange = DateRange(request.startDate, request.endDate)
      val dateRange = DateRange(DateTime.now().minusDays(30), DateTime.now())
      RankingParams(dateRange, length, AuthParams(token))
    }

    def toXorResponse(proc: Proc.Reload.XorResponse): Api.Reload.XorResponse =
      proc.bimap(
        err ⇒ Api.Reload.Error(err.code, err.message, err.status),
        res ⇒ Api.Reload.Response()
      )
  }

}