package cards.nine.services.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.domain.application.Package
import cards.nine.domain.pagination.Page
import cards.nine.services.free.domain
import cards.nine.services.free.interpreter.collection.Services.SharedCollectionData

object SharedCollection {

  sealed trait Ops[A]

  case class Add(collection: SharedCollectionData) extends Ops[domain.SharedCollection]

  case class GetById(id: Long) extends Ops[Option[domain.SharedCollection]]

  case class GetByPublicId(publicId: String) extends Ops[Option[domain.SharedCollection]]

  case class GetByUser(user: Long) extends Ops[List[domain.SharedCollectionWithAggregatedInfo]]

  case class GetLatestByCategory(category: String, pageParams: Page) extends Ops[List[domain.SharedCollection]]

  case class GetTopByCategory(category: String, pageParams: Page) extends Ops[List[domain.SharedCollection]]

  case class Update(id: Long, title: String) extends Ops[Int]

  case class UpdatePackages(collection: Long, packages: List[Package]) extends Ops[(List[Package], List[Package])]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(collection: SharedCollectionData): Free[F, domain.SharedCollection] =
      Free.inject[Ops, F](Add(collection))

    def getById(id: Long): Free[F, Option[domain.SharedCollection]] = Free.inject[Ops, F](GetById(id))

    def getByPublicId(publicId: String): Free[F, Option[domain.SharedCollection]] =
      Free.inject[Ops, F](GetByPublicId(publicId))

    def getByUser(user: Long): Free[F, List[domain.SharedCollectionWithAggregatedInfo]] =
      Free.inject[Ops, F](GetByUser(user))

    def getLatestByCategory(category: String, pageParams: Page): Free[F, List[domain.SharedCollection]] =
      Free.inject[Ops, F](GetLatestByCategory(category, pageParams))

    def getTopByCategory(category: String, pageParams: Page): Free[F, List[domain.SharedCollection]] =
      Free.inject[Ops, F](GetTopByCategory(category, pageParams))

    def update(id: Long, title: String): Free[F, Int] = Free.inject[Ops, F](Update(id, title))

    def updatePackages(collection: Long, packages: List[Package]): Free[F, (List[Package], List[Package])] =
      Free.inject[Ops, F](UpdatePackages(collection, packages))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services[F]

  }

}
