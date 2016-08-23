package com.fortysevendeg.ninecards.processes

import cats.data.{ Xor, XorT }
import cats.free.Free
import cats.Monad
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.converters.Converters._
import com.fortysevendeg.ninecards.processes.messages.rankings._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.utils.XorTSyntax._
import com.fortysevendeg.ninecards.processes.utils.MonadInstances._
import com.fortysevendeg.ninecards.services.common.ConnectionIOOps._
import com.fortysevendeg.ninecards.services.free.algebra.DBResult.DBOps
import com.fortysevendeg.ninecards.services.free.algebra.GoogleAnalytics
import com.fortysevendeg.ninecards.services.free.domain.rankings._
import com.fortysevendeg.ninecards.services.persistence._
import com.fortysevendeg.ninecards.services.persistence.rankings.{ Services ⇒ PersistenceServices }
import doobie.imports._
import scalaz.concurrent.Task

class RankingProcesses[F[_]](
  implicit
  analytics: GoogleAnalytics.Services[F],
  persistence: PersistenceServices,
  transactor: Transactor[Task],
  dbOps: DBOps[F]
) {

  def getRanking(scope: GeoScope): Free[F, Get.Response] =
    persistence.getRanking(scope).map(Get.Response.apply).liftF[F]

  def reloadRanking(scope: GeoScope, params: RankingParams): Free[F, Reload.XorResponse] =
    for /*Free[F]*/ {
      rankingTry ← analytics.getRanking(scope, params)
      res ← rankingTry match {
        case Xor.Left(error) ⇒ errorAux(error)
        case Xor.Right(ranking) ⇒ setAux(scope, ranking)
      }
    } yield res

  private[this] def setAux(scope: GeoScope, ranking: Ranking): Free[F, Reload.XorResponse] =
    persistence.setRanking(scope, ranking).liftF[F].map(_ ⇒ Xor.Right(Reload.Response()))

  private[this] def errorAux(error: RankingError): Free[F, Reload.XorResponse] = {
    val procError = Reload.Error(error.code, error.message, error.status)
    Free.pure[F, Reload.XorResponse](Xor.Left(procError))
  }

}

object RankingProcesses {

  implicit def processes[F[_]](
    implicit
    analytics: GoogleAnalytics.Services[F],
    persistence: PersistenceServices,
    dbOps: DBOps[F]
  ) = new RankingProcesses

}