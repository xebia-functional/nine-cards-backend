package cards.nine.services.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.services.free.domain
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData

object SharedCollection {

  sealed trait Ops[A]

  case class Add(collection: SharedCollectionData) extends Ops[domain.SharedCollection]

  case class AddPackages(collection: Long, packages: List[String]) extends Ops[Int]

  case class GetById(id: Long) extends Ops[Option[domain.SharedCollection]]

  case class GetByPublicId(publicId: String) extends Ops[Option[domain.SharedCollection]]

  case class GetByUser(user: Long) extends Ops[List[domain.SharedCollectionWithAggregatedInfo]]

  case class GetLatestByCategory(category: String, pageNumber: Int, pageSize: Int) extends Ops[List[domain.SharedCollection]]

  case class GetTopByCategory(category: String, pageNumber: Int, pageSize: Int) extends Ops[List[domain.SharedCollection]]

  case class GetPackagesByCollection(collection: Long) extends Ops[List[domain.SharedCollectionPackage]]

  case class Update(id: Long, title: String) extends Ops[Int]

  case class UpdatePackages(collection: Long, packages: List[String]) extends Ops[(List[String], List[String])]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(collection: SharedCollectionData): Free[F, domain.SharedCollection] =
      Free.inject[Ops, F](Add(collection))

    def addPackages(collection: Long, packages: List[String]): Free[F, Int] =
      Free.inject[Ops, F](AddPackages(collection, packages))

    def getById(id: Long): Free[F, Option[domain.SharedCollection]] = Free.inject[Ops, F](GetById(id))

    def getByPublicId(publicId: String): Free[F, Option[domain.SharedCollection]] =
      Free.inject[Ops, F](GetByPublicId(publicId))

    def getByUser(user: Long): Free[F, List[domain.SharedCollectionWithAggregatedInfo]] =
      Free.inject[Ops, F](GetByUser(user))

    def getLatestByCategory(category: String, pageNumber: Int, pageSize: Int): Free[F, List[domain.SharedCollection]] =
      Free.inject[Ops, F](GetLatestByCategory(category, pageNumber, pageSize))

    def getTopByCategory(category: String, pageNumber: Int, pageSize: Int): Free[F, List[domain.SharedCollection]] =
      Free.inject[Ops, F](GetTopByCategory(category, pageNumber, pageSize))

    def getPackagesByCollection(collection: Long): Free[F, List[domain.SharedCollectionPackage]] =
      Free.inject[Ops, F](GetPackagesByCollection(collection))

    def update(id: Long, title: String): Free[F, Int] = Free.inject[Ops, F](Update(id, title))

    def updatePackages(collection: Long, packages: List[String]): Free[F, (List[String], List[String])] =
      Free.inject[Ops, F](UpdatePackages(collection, packages))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services[F]

  }

}
