package com.fortysevendeg.ninecards.free.interpreter

import com.fortysevendeg.ninecards.free.algebra.appsGooglePlay._
import com.fortysevendeg.ninecards.free.algebra.appsPersistence._
import com.fortysevendeg.ninecards.free.algebra.sharedCollectionSubscriptions._
import com.fortysevendeg.ninecards.free.algebra.sharedCollections._
import com.fortysevendeg.ninecards.free.algebra.user._
import com.fortysevendeg.ninecards.free.domain.{User, SharedCollectionSubscription, SharedCollection}

import scalaz._

object interpreters {

  def or[F[_], G[_], H[_]](f: F ~> H, g: G ~> H): ({type cp[α] = Coproduct[F, G, α]})#cp ~> H = {
    new NaturalTransformation[({type cp[α] = Coproduct[F, G, α]})#cp, H] {
      def apply[A](fa: Coproduct[F, G, A]): H[A] = {
        fa.run match {
          case -\/(ff) => f(ff)
          case \/-(gg) => g(gg)
        }
      }
    }
  }

  object AppGooglePlayInterpreter extends (AppGooglePlayOps ~> Id.Id) {
    def apply[A](fa: AppGooglePlayOps[A]) = fa match {
      case GetCategoriesFromGooglePlay(packageNames: Seq[String]) => {
        println("Running GetCategoriesFromGooglePlay ...")
        Seq("SOCIAL")
      }
    }
  }

  object AppPersistenceInterpreter extends (AppPersistenceOps ~> Id.Id) {
    def apply[A](fa: AppPersistenceOps[A]) = fa match {
      case GetCategories(packageNames: Seq[String]) => {
        println("Running GetCategories ...")
        Seq("COMMUNICATION")
      }
      case SaveCategories(packageNames: Seq[String]) => Seq("GAMES")
    }
  }

  object SharedCollectionInterpreter extends (SharedCollectionOps ~> Id.Id) {

    def apply[A](fa: SharedCollectionOps[A]) = fa match {
      case AddSharedCollection(collection: SharedCollection) => collection
      case GetSharedCollectionById(collectionId: String) => Option(SharedCollection(sharedCollectionId = None, name = "Collection", resolvedPackages = Seq.empty))
      case UpdateInstallNotification(collectionId: String) => SharedCollection(sharedCollectionId = None, name = "Collection", resolvedPackages = Seq.empty)
      case UpdateViewNotification(collectionId: String) => SharedCollection(sharedCollectionId = None, name = "Collection", resolvedPackages = Seq.empty)
    }
  }

  object SharedCollectionSubscriptionInterpreter extends (SharedCollectionSubscriptionOps ~> Id.Id) {

    def apply[A](fa: SharedCollectionSubscriptionOps[A]) = fa match {
      case AddSharedCollectionSubscription(collectionId: String) => SharedCollectionSubscription(sharedCollectionId = collectionId, userId = "userId")
      case DeleteSharedCollectionSubscription(collectionId: String) => ()
    }
  }

  object UserInterpreter extends (UserOps ~> Id.Id) {

    def apply[A](fa: UserOps[A]) = fa match {
      case AddUser(user: User) => user
      case GetUserByUserName(username: String) => Option(User())
      case CheckPassword(pass: String) => true
    }
  }
}
