package cards.nine.services.free.algebra

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService.{ NineCardsService, Result }
import cards.nine.services.free.domain.SharedCollectionSubscription
import cats.free.:<:

object Subscription {

  sealed trait Ops[A]

  case class Add(collection: Long, user: Long, collectionPublicId: String) extends Ops[Result[Int]]

  case class GetByCollectionAndUser(collection: Long, user: Long) extends Ops[Result[Option[SharedCollectionSubscription]]]

  case class GetByCollection(collection: Long) extends Ops[Result[List[SharedCollectionSubscription]]]

  case class GetByUser(user: Long) extends Ops[Result[List[SharedCollectionSubscription]]]

  case class RemoveByCollectionAndUser(collection: Long, user: Long) extends Ops[Result[Int]]

  class Services[F[_]](implicit I: Ops :<: F) {

    def add(collection: Long, user: Long, collectionPublicId: String): NineCardsService[F, Int] =
      NineCardsService(Add(collection, user, collectionPublicId))

    def getByCollectionAndUser(collection: Long, user: Long): NineCardsService[F, Option[SharedCollectionSubscription]] =
      NineCardsService(GetByCollectionAndUser(collection, user))

    def getByCollection(collection: Long): NineCardsService[F, List[SharedCollectionSubscription]] =
      NineCardsService(GetByCollection(collection))

    def getByUser(user: Long): NineCardsService[F, List[SharedCollectionSubscription]] =
      NineCardsService(GetByUser(user))

    def removeByCollectionAndUser(collection: Long, user: Long): NineCardsService[F, Int] =
      NineCardsService(RemoveByCollectionAndUser(collection, user))
  }

  object Services {

    implicit def services[F[_]](implicit I: Ops :<: F): Services[F] = new Services

  }

}
