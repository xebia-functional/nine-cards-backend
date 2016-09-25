package cards.nine.services.free.algebra

import cards.nine.services.free.domain.SharedCollectionSubscription
import cats.free.{ Free, Inject }

object Subscription {

  sealed trait Ops[A]

  case class Add(collection: Long, user: Long, collectionPublicId: String) extends Ops[Int]

  case class GetByCollectionAndUser(collection: Long, user: Long) extends Ops[Option[SharedCollectionSubscription]]

  case class GetByCollection(collection: Long) extends Ops[List[SharedCollectionSubscription]]

  case class GetByUser(user: Long) extends Ops[List[SharedCollectionSubscription]]

  case class RemoveByCollectionAndUser(collection: Long, user: Long) extends Ops[Int]

  class Services[F[_]](implicit I: Inject[Ops, F]) {

    def add(collection: Long, user: Long, collectionPublicId: String): Free[F, Int] =
      Free.inject[Ops, F](Add(collection, user, collectionPublicId))

    def getByCollectionAndUser(collection: Long, user: Long): Free[F, Option[SharedCollectionSubscription]] =
      Free.inject[Ops, F](GetByCollectionAndUser(collection, user))

    def getByCollection(collection: Long): Free[F, List[SharedCollectionSubscription]] =
      Free.inject[Ops, F](GetByCollection(collection))

    def getByUser(user: Long): Free[F, List[SharedCollectionSubscription]] =
      Free.inject[Ops, F](GetByUser(user))

    def removeByCollectionAndUser(collection: Long, user: Long): Free[F, Int] =
      Free.inject[Ops, F](RemoveByCollectionAndUser(collection, user))
  }

  object Services {

    implicit def services[F[_]](implicit I: Inject[Ops, F]): Services[F] = new Services

  }

}
