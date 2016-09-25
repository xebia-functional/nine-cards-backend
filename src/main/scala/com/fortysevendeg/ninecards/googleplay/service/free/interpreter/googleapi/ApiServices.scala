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

  def getCard(request: AppRequest): Task[InfoError Xor FullCard] =
    getCard(request.packageName, request.authParams)

  def recommendByCategory( request: RecommendationsByCategory ) : Task[InfoError Xor FullCardList] = {
    import request._

    lazy val infoError = InfoError( s"Recommendations for category $category that are $filter")

    val idsT: Task[InfoError Xor List[String]] =
      apiClient.list( category, filter, auth).map( _.bimap(
        _res => infoError,
        lres => Converters.listResponseToPackages(lres)
      ))

    idsT flatMap {
      case left@Xor.Left(_) => Task.now(left)
      case Xor.Right(ids) => getCards(ids, auth).map(Xor.Right.apply)
    }

  }

  def recommendByAppList( request: RecommendationsByAppList) : Task[FullCardList] = {
    import request._

    val recommendedPackages: Task[List[String]] = {
      for /* Task */ {
        xors <- packageList.items.traverse { pack =>
          apiClient.recommendations(Package(pack), auth)
        }
        ids = Converters.listResponseListToPackages( splitXors(xors)._2)
      } yield ids
    }

    for /* Task */ {
      ids <- recommendedPackages
      appRecList <- getCards(ids, auth)
    } yield appRecList
  }

  private[this] def getCards(ids: List[String], auth: GoogleAuthParams ) : Task[FullCardList] =
    ids.traverse(id => getCard(Package(id), auth))
      .map( Converters.toFullCardListXors)

  private[this] def getCard(pack: Package, auth: GoogleAuthParams): Task[Xor[InfoError, FullCard]] =
    appService( AppRequest(pack, auth) )

}
