package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import com.fortysevendeg.extracats.{taskMonad, splitXors}
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import scalaz.concurrent.Task

case class ApiServices(apiClient: ApiClient, appService: AppRequest => Task[InfoError Xor FullCard] ) {

  def getItem(request: AppRequest): Task[Xor[String, Item]] =
    apiClient.details(request.packageName, request.authParams).map { xor =>
      xor.bimap(
        _res => request.packageName.value,
        docV2 => Converters.toItem(docV2)
      )
    }

  def recommendByCategory( message: RecommendationsByCategory ) : Task[InfoError Xor FullCardList] = {
    import message._
    import request._

    lazy val infoError = InfoError( s"Recommendations for category $category that are $priceFilter")

    apiClient.list( request.category, request.priceFilter, auth) flatMap {
      case Xor.Left(_) => Task.now(Xor.Left(infoError))

      case Xor.Right(listResponse) =>
        val ids = Converters.listResponseToPackages(listResponse)
        val filteredIds = ids.diff(excludedApps).take(maxTotal)
        getCards(filteredIds, auth).map(Xor.Right.apply)
    }

  }

  def recommendByAppList( message: RecommendationsByAppList) : Task[FullCardList] = {
    import message.request._

    def joinLists(xors: List[InfoError Xor List[Package]]): List[Package] = {
      val lists = splitXors(xors)._2
      lists.flatten.distinct.diff(excludedApps).take(maxTotal)
    }

    val recommendedPackages: Task[List[Package]] =
      searchByApps
        .traverse( recommendByApp(message.auth, numPerApp, _) )
        .map( joinLists)

    for /*Task*/ {
      ids <- recommendedPackages
      filteredIds = ids.diff(excludedApps).take(maxTotal)
      cards <- getCards( filteredIds, message.auth)
    } yield cards
  }

  private[this] def getCards(packs: List[Package], auth: GoogleAuthParams ) : Task[FullCardList] =
    packs
      .traverse(pack => appService( AppRequest(pack, auth)))
      .map( Converters.toFullCardListXors )

  def recommendByApp( auth: GoogleAuthParams, numPerApp: Int, pack: Package)
      : Task[InfoError Xor List[Package]] =
    apiClient.recommendations(pack, auth).map(_.bimap(
      _resp   => InfoError( s"Recommendations for package ${pack.value}"), 
      listRes => Converters.listResponseToPackages(listRes).take(numPerApp)
    ))

}
