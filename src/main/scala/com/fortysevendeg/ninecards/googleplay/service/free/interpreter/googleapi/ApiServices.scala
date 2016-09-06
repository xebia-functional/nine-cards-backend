package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import cats.std.list._
import cats.syntax.traverse._
import com.fortysevendeg.extracats.{taskMonad, splitXors}
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import scalaz.concurrent.Task

class ApiServices( apiClient: ApiClient) {

  def getItem(request: AppRequest): Task[Xor[String, Item]] =
    apiClient.details(request.packageName, request.authParams).map { xor =>
      xor.bimap(
        _res => request.packageName.value,
        docV2 => Converters.toItem(docV2)
      )
    }

  def getCard(request: AppRequest): Task[Xor[InfoError, AppCard]] =
    apiClient.details(request.packageName, request.authParams).map { xor =>
      xor.bimap(
        _ => InfoError(request.packageName.value),
        docV2 => Converters.toCard(docV2)
      )
    }

  def recommendByCategory( request: RecommendationsByCategory ) : Task[InfoError Xor AppRecommendationList] = {
    import request._

    lazy val infoError = InfoError( s"Recommendations for category $category that are $filter")

    val idsT: Task[Xor[InfoError,List[String]]] =
      apiClient.list( category, filter, auth).map( _.bimap(
        _res => infoError,
        lres => Converters.listResponseToPackages(lres)
      ))

    for /*Task*/ {
      ids <- idsT
      res <- ids match {
        case left@Xor.Left(_) => Task.now(left)
        case Xor.Right(ids) => getRecommendationList(ids, auth).map(Xor.Right.apply)
      }
    } yield res

  }

  def recommendByAppList( request: RecommendationsByAppList) : Task[AppRecommendationList] = {
    import request._
    for /* Task */ {
      xors <- packageList.items.traverse { pack =>
        apiClient.recommendations(Package(pack), auth)
      }
      ids = Converters.listResponseListToPackages( splitXors(xors)._2)
      appRecList <- getRecommendationList(ids, auth)
    } yield appRecList
  }

  private[this] def getRecommendationList(
    ids: List[String], auth: GoogleAuthParams
  ) : Task[AppRecommendationList] =
    for {
      xors <- ids traverse (id => apiClient.details( Package(id), auth))
      docV2s = splitXors(xors)._2
    } yield Converters.toAppRecommendationList(docV2s)


}
