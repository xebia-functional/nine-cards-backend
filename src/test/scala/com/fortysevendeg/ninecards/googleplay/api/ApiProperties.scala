package com.fortysevendeg.ninecards.googleplay.api

import cats.{ Id, ~>  }
import cats.data.Xor
import cats.syntax.option._
import com.fortysevendeg.extracats.splitXors
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay._
import io.circe.generic.auto._
import io.circe.parser._
import org.scalacheck.Prop._
import org.scalacheck._
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpRequest
import spray.http.StatusCodes._
import spray.testkit.{RouteTest, TestFrameworkInterface}
import spray.routing.{ HttpService, Route }

class MockInterpreter(
  resolveOne: Package => Option[Item],
  resolveMany: PackageList => PackageDetails,
  getCard: Package => Xor[InfoError, FullCard],
  getCardList: PackageList => FullCardList,
  recommendByCategory: RecommendationsByCategory => Xor[InfoError, FullCardList],
  recommendByAppList: RecommendationsByAppList => FullCardList
) extends (Ops ~> Id) {

  def apply[A](fa: Ops[A]): Id[A] = fa match {
    case Resolve(_, p) => resolveOne(p)
    case ResolveMany(_, pl) => resolveMany(pl)
    case GetCard(_, p) => getCard(p)
    case GetCardList(_, pl) => getCardList(pl)
    case rec@RecommendationsByCategory(_,_,_) => recommendByCategory(rec)
    case rec@RecommendationsByAppList(_,_) => recommendByAppList(rec)
  }
}

object MockInterpreter {

  private[this] def failFunction[A, B](message: String): (A => B) =
    ( _ => throw new RuntimeException(message) )

  private[this] val failResolveOne: (Package => Option[Item]) =
    failFunction[Package, Option[Item]](s"Should not ask for the Item of a package")

  private[this] val failResolveMany: (PackageList => PackageDetails) =
    failFunction[PackageList, PackageDetails]("Should not ask to ResolveMany packages")

  private[this] val failGetCard: Package => Xor[InfoError, FullCard] =
    failFunction[Package, Xor[InfoError, FullCard]]("Should not ask to GetCard for a package")

  private[this] val failGetCardList: PackageList => FullCardList =
    failFunction[PackageList, FullCardList]("Should not ask to GetCards for a List of packages")

  private[this] val failRecommendByCategory: RecommendationsByCategory => Xor[InfoError, FullCardList] =
    failFunction("Should not ask for the Recommendations of a Category")

  private[this] val failRecommendByAppList: RecommendationsByAppList => FullCardList =
    failFunction("Should not ask for Recommendations of app list")

  def apply(
    resolveOne: Package => Option[Item] = failResolveOne,
    resolveMany: PackageList => PackageDetails = failResolveMany,
    getCard: Package => Xor[InfoError, FullCard] = failGetCard,
    getCardList: PackageList => FullCardList = failGetCardList,
    recommendByCategory: RecommendationsByCategory => Xor[InfoError, FullCardList] = failRecommendByCategory,
    recommendByAppList: RecommendationsByAppList => FullCardList = failRecommendByAppList
  ): MockInterpreter =
    new MockInterpreter(resolveOne, resolveMany,getCard, getCardList, recommendByCategory, recommendByAppList )

}

trait ScalaCheckRouteTest extends RouteTest with TestFrameworkInterface {
  def failTest(msg: String): Nothing = throw new RuntimeException(msg)
}

object ApiProperties
    extends Properties("Nine Cards Google Play API")
    with ScalaCheckRouteTest
    with HttpService {

  override implicit def actorRefFactory = system

  def ifOption[A]( dec: Boolean, a: A) : Option[A] = if (dec) Some(a) else None
  def xorThenElse[A,B]( dec: Boolean, a: A, b: B): Xor[A,B] = if(dec) Xor.Left(a) else Xor.Right(b)

  import NineCardsMarshallers._
  import AuthHeadersRejectionHandler._

  def makeRoute(interpreter: MockInterpreter = MockInterpreter() ): Route = {
    val marshallerFactory: TRMFactory[FreeOps] =
      contraNaturalTransformFreeTRMFactory[Ops, Id](interpreter, Id, IdMarshallerFactory)
    val api = new NineCardsGooglePlayApi[Ops]()(Service.service, marshallerFactory)
    sealRoute(api.googlePlayApiRoute )
  }

  val requestHeaders = List(
    RawHeader(Headers.androidId, "androidId"),
    RawHeader(Headers.token, "googlePlayToken"),
    RawHeader(Headers.localization, "es-ES")
  )

  import com.fortysevendeg.ninecards.googleplay.util.ScalaCheck._

  def splitFind[K,V]( database: Map[K,V], keys: List[K]) : (List[K], List[V]) =
    splitXors[K,V](keys.map( k => database.get(k).toRightXor(k)) )

  def resolveOne(pkg: Package) =
    Get(s"/googleplay/package/${pkg.value}")

  property(s" ${endpoints.item} returns the correct package name for a Google Play Store app") =
    forAll { (pkg: Package, item: Item) =>

      val route = makeRoute(MockInterpreter(
        resolveOne = { p => ifOption(p == pkg, item) }
      ))
      resolveOne(pkg) ~> addHeaders(requestHeaders) ~> route ~> check {
        val response = responseAs[String]
          (status ?= OK) && (decode[Item](response) ?= Xor.Right(item))
      }
    }

  property(s"${endpoints.item} fails with Unauthorized if headers are missing" ) =
    forAll ( (pkg: Package) => checkUnauthorized( resolveOne(pkg)  ))

  property(s"${endpoints.item} fails with an Internal Server Error when the package is not known") =
    forAll {(unknownPackage: Package, wrongItem: Item) =>

      val route = makeRoute(MockInterpreter(
        resolveOne = ( p => ifOption( p != unknownPackage, wrongItem))
      ))
      resolveOne(unknownPackage) ~> addHeaders(requestHeaders) ~> route ~> check {
        status ?= InternalServerError
      }
    }

  def resolveMany(packageList: PackageList) = Post("/googleplay/packages/detailed", packageList)

  property(s"${endpoints.itemList} gives the package details for the known packages and highlights the errors") =
    forAll(genPick[Package, Item]) { (data: (Map[Package, Item], List[Package], List[Package])) =>

      val (database, succs, errs) = data

      //order doesn't matter
      val errors = errs.map(_.value).toSet
      val items = succs.map(i => database(i)).toSet

      val route = makeRoute( MockInterpreter( resolveMany = {
        case PackageList(packages) =>
          val (errors, items) = splitFind[Package,Item](database, packages.map(Package.apply))
          PackageDetails(errors.map(_.value), items)
      }))

      val allPackages = (succs ++ errs).map(_.value)

      resolveMany( PackageList(allPackages)) ~> addHeaders(requestHeaders) ~> route ~> check {
        val response = responseAs[String]
        val decoded = decode[PackageDetails](response)
          .getOrElse(throw new RuntimeException(s"Unable to parse response [$response]"))
        (status ?= OK) &&
        (decoded.errors.toSet ?= errors) &&
        (decoded.items.toSet ?= items)
      }
    }

  property(s"${endpoints.itemList} fails with Unauthorized if auth headers are missing") =
    forAll { (packages: List[Package]) =>
      checkUnauthorized( resolveMany(PackageList(packages.map(_.value))) )
    }

  def getCard(pkg: Package) = Get(s"/googleplay/cards/${pkg.value}") ~> addHeaders(requestHeaders)

  property(s"${endpoints.card} returns a valid card for an app") =
    forAll { (pkg: Package, card: FullCard) =>
      val apiCard = Converters.toApiCard(card)

      val route = makeRoute(MockInterpreter( getCard = { (p: Package) =>
        xorThenElse( p != pkg, InfoError(p.value), card)
      }))

      getCard(pkg) ~> route ~> check {
        val response = responseAs[String]
          (status ?= OK) && (decode[ApiCard](response) ?= Xor.Right( apiCard ))
      }
    }

  property(s"${endpoints.card} fails with a NotFound Error when the package is not known") =
    forAll {(unknownPackage: Package, wrongCard: FullCard) =>

      val infoError = InfoError(unknownPackage.value)
      val getCardSer = ( (p: Package) => xorThenElse( p == unknownPackage, infoError, wrongCard) )
      val route = makeRoute(MockInterpreter( getCard = getCardSer ))

      getCard(unknownPackage) ~> route ~> check {
        val response = responseAs[String]
        (status ?= NotFound) && (decode[InfoError](response) ?= Xor.Right(infoError) )
      }
    }

  def checkUnauthorized(req: HttpRequest) =
    req ~> makeRoute(MockInterpreter()) ~> check (status ?= Unauthorized)

  def getCardList(pkg: PackageList) = Post(s"/googleplay/cards", pkg) 

  property(s"${endpoints.cardList} fails with Unauthorized if auth headers are missing") =
    forAll { (packages: List[Package]) =>
      checkUnauthorized( getCardList(PackageList(packages.map(_.value))) )
    }

  property(s"${endpoints.cardList} fails with a NotFound Error when the package is not known") =
    forAll(genPick[Package, FullCard]) { (data: (Map[Package, FullCard], List[Package], List[Package])) =>

      val (database, succs, errs) = data

      //order doesn't matter
      val errors = errs.map(_.value).toSet
      val items = succs.map(i => database(i)).toSet

      val getCardListSer: PackageList => FullCardList = { case PackageList(packages) =>
        val (errors, items) = splitFind[Package,FullCard](database, packages.map(Package.apply))
        FullCardList(errors.map(_.value), items)
      }

      val route = makeRoute( MockInterpreter( getCardList = getCardListSer) )

      val allPackages = (succs ++ errs).map(_.value)

      getCardList( PackageList(allPackages)) ~> addHeaders(requestHeaders) ~> route ~> check {
        val response = responseAs[String]
        val decoded = decode[ApiCardList](response)
          .getOrElse(throw new RuntimeException(s"Unable to parse response [$response]"))
        (status ?= OK) &&
        (decoded.missing.toSet ?= errors) &&
        (decoded.apps.toSet ?= (items map Converters.toApiCard) )
      }
    }

  def recommendByCategory( category: String, filter: String) =
    Get(s"/googleplay/recommendations/$category/$filter") ~> addHeaders(requestHeaders)

  property( s" ${endpoints.recommendCategory} fails with NotFound if the category does not exist")  = {
    val route = makeRoute()
    forAll { (category: PathSegment , filter: PriceFilter) => {
      val seg = category.value
      ( ! seg.isEmpty && Category.withNameOption(seg).isEmpty) ==> {
        recommendByCategory( seg, filter.toString) ~> addHeaders(requestHeaders)~> route ~> check {
          status ?= NotFound
        }
      }}
    }
  }

  property( s" ${endpoints.recommendCategory} fails with NotFound if the price filter is wrong")  = {
    val route = makeRoute()
    forAll { (category: Category, filter: PathSegment) => {
      val seg = filter.value
      (! seg.isEmpty && PriceFilter.withNameOption(seg).isEmpty) ==> {
        recommendByCategory(category.toString, seg) ~> addHeaders(requestHeaders) ~> route ~> check {
          status ?= NotFound
        }
      }
    }}
  }

  property( s"${endpoints.recommendCategory} fails with Unauthorized if the auth headers are missing") = {
    val route = makeRoute()
    forAll { (category: Category, filter: PriceFilter) => {
      checkUnauthorized( Get(s"/googleplay/recommendations/$category/$filter") )
    }}
  }

  def recommendByAppList(packageList: PackageList) = Post("/googleplay/recommendations", packageList)

  property( s"${endpoints.recommendAppList} fails with Unauthorized if the auth headers are missing") =
    forAll { (packages: List[Package]) =>
      checkUnauthorized( recommendByAppList(PackageList(packages.map(_.value))) )
    }

}
