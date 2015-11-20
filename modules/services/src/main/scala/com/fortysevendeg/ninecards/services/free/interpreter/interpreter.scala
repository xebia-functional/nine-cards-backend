package com.fortysevendeg.ninecards.services.free.interpreter

import com.fortysevendeg.ninecards.services.free.algebra.AppGooglePlay._
import com.fortysevendeg.ninecards.services.free.algebra.AppPersistence._
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollection._
import com.fortysevendeg.ninecards.services.free.algebra.SharedCollectionSubscription._
import com.fortysevendeg.ninecards.services.free.algebra.User._
import com.fortysevendeg.ninecards.services.free.domain._

import scalaz._

object Interpreters {

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
      case GetCategoriesFromGooglePlay(packageNames: Seq[String]) =>
        CategorizeResponse(
          categorizedApps = Seq(GooglePlayApp(
            packageName = "com.android.chrome",
            appType = "",
            appCategory = "COMMUNICATION",
            numDownloads = "1,000,000,000 - 5,000,000,000",
            starRating = 4.235081672668457,
            ratingCount = 3715846,
            commentCount = 0)),
          notFoundApps = Seq("com.facebook.orca"))
    }
  }

  object AppPersistenceInterpreter extends (AppPersistenceOps ~> Id.Id) {
    def apply[A](fa: AppPersistenceOps[A]) = fa match {
      case GetCategories(packageNames: Seq[String]) =>
        CategorizeResponse(
          categorizedApps = Seq(GooglePlayApp(
            packageName = "com.whatsapp",
            appType = "",
            appCategory = "COMMUNICATION",
            numDownloads = "1,000,000,000 - 5,000,000,000",
            starRating = 4.433322429656982,
            ratingCount = 31677777,
            commentCount = 0)),
          notFoundApps = Seq("com.skype.raider"))
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
