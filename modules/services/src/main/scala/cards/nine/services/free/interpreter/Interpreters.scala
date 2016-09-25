package cards.nine.services.free.interpreter

import cats._
import cards.nine.services.free.algebra._
import cards.nine.services.free.domain
import cards.nine.services.free.interpreter.analytics.{ Services ⇒ AnalyticsServices }
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.firebase.{ Services ⇒ FirebaseServices }
import cards.nine.services.free.interpreter.googleapi.{ Services ⇒ GoogleApiServices }
import cards.nine.services.free.interpreter.googleplay.{ Services ⇒ GooglePlayServices }
import cards.nine.services.free.interpreter.ranking.{ Services ⇒ RankingServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import cards.nine.services.persistence.CustomComposite
import cards.nine.services.persistence.DatabaseTransactor._
import doobie.imports._

import scalaz.concurrent.Task

abstract class Interpreters[M[_]](implicit A: ApplicativeError[M, Throwable], T: Transactor[M]) {

  val task2M: (Task ~> M)

  object googleApiInterpreter extends (GoogleApi.Ops ~> M) {
    private[this] val googleApiServices: GoogleApiServices = GoogleApiServices.services

    def apply[A](fa: GoogleApi.Ops[A]) = fa match {
      case GoogleApi.GetTokenInfo(tokenId: String) ⇒ task2M(googleApiServices.getTokenInfo(tokenId))
    }
  }

  object googlePlayInterpreter extends (GooglePlay.Ops ~> M) {
    private[this] val googlePlayServices: GooglePlayServices = GooglePlayServices.services

    def apply[A](fa: GooglePlay.Ops[A]) = fa match {
      case GooglePlay.ResolveMany(packageNames, auth) ⇒
        task2M(googlePlayServices.resolveMany(packageNames, auth))
      case GooglePlay.Resolve(packageName, auth) ⇒
        task2M(googlePlayServices.resolveOne(packageName, auth))
      case GooglePlay.RecommendationsByCategory(category, filter, auth) ⇒
        task2M(googlePlayServices.recommendByCategory(category, filter, auth))
      case GooglePlay.RecommendationsForApps(packagesName, auth) ⇒
        task2M(googlePlayServices.recommendationsForApps(packagesName, auth))
    }
  }

  object analyticsInterpreter extends (GoogleAnalytics.Ops ~> M) {
    private[this] val services: AnalyticsServices = AnalyticsServices.services

    import GoogleAnalytics._

    def apply[A](fa: Ops[A]): M[A] = {
      val task: Task[A] = fa match {
        case GetRanking(geoScope, params) ⇒ services.getRanking(geoScope, params)
      }
      task2M(task)
    }
  }

  object firebaseInterpreter extends (Firebase.Ops ~> M) {
    private[this] val firebaseServices: FirebaseServices = FirebaseServices.services

    def apply[A](fa: Firebase.Ops[A]) = fa match {
      case Firebase.SendUpdatedCollectionNotification(info) ⇒
        task2M {
          firebaseServices.sendUpdatedCollectionNotification(info)
        }
    }
  }

  object collectionInterpreter extends (SharedCollection.Ops ~> M) {

    private[this] val collectionServices: CollectionServices = CollectionServices.services

    def apply[A](fa: SharedCollection.Ops[A]) = fa match {
      case SharedCollection.Add(collection) ⇒
        collectionServices.add[domain.SharedCollection](collection).transact(T)
      case SharedCollection.AddPackages(collection, packages) ⇒
        collectionServices.addPackages(collection, packages).transact(T)
      case SharedCollection.GetById(id) ⇒
        collectionServices.getById(id).transact(T)
      case SharedCollection.GetByPublicId(publicId) ⇒
        collectionServices.getByPublicIdentifier(publicId).transact(T)
      case SharedCollection.GetByUser(user) ⇒
        collectionServices.getByUser(user).transact(T)
      case SharedCollection.GetLatestByCategory(category, pageNumber, pageSize) ⇒
        collectionServices.getLatestByCategory(category, pageNumber, pageSize).transact(T)
      case SharedCollection.GetPackagesByCollection(collection) ⇒
        collectionServices.getPackagesByCollection(collection).transact(T)
      case SharedCollection.GetTopByCategory(category, pageNumber, pageSize) ⇒
        collectionServices.getTopByCategory(category, pageNumber, pageSize).transact(T)
      case SharedCollection.Update(id, title) ⇒
        collectionServices.updateCollectionInfo(id, title).transact(T)
      case SharedCollection.UpdatePackages(collection, packages) ⇒
        collectionServices.updatePackages(collection, packages).transact(T)
    }
  }

  object countryInterpreter extends (Country.Ops ~> M) {
    private[this] val countryServices: CountryServices = CountryServices.services

    def apply[A](fa: Country.Ops[A]) = fa match {
      case Country.GetCountryByIsoCode2(isoCode) ⇒
        countryServices.getCountryByIsoCode2(isoCode).transact(T)
    }
  }

  object rankingInterpreter extends (Ranking.Ops ~> M) {
    import CustomComposite._

    private[this] val rankingServices: RankingServices = RankingServices.services

    def apply[A](fa: Ranking.Ops[A]) = fa match {
      case Ranking.GetRankingForApps(scope, apps) ⇒
        rankingServices.getRankingForApps(scope, apps).transact(T)
      case Ranking.GetRanking(scope) ⇒
        rankingServices.getRanking(scope).transact(T)
      case Ranking.UpdateRanking(scope, ranking) ⇒
        rankingServices.updateRanking(scope, ranking).transact(T)
    }
  }

  object subscriptionInterpreter extends (Subscription.Ops ~> M) {

    private[this] val subscriptionServices: SubscriptionServices = SubscriptionServices.services

    def apply[A](fa: Subscription.Ops[A]) = fa match {
      case Subscription.Add(collection, user, collectionPublicId) ⇒
        subscriptionServices.add(collection, user, collectionPublicId).transact(T)
      case Subscription.GetByCollection(collection) ⇒
        subscriptionServices.getByCollection(collection).transact(T)
      case Subscription.GetByCollectionAndUser(collection, user) ⇒
        subscriptionServices.getByCollectionAndUser(collection, user).transact(T)
      case Subscription.GetByUser(user) ⇒
        subscriptionServices.getByUser(user).transact(T)
      case Subscription.RemoveByCollectionAndUser(collection, user) ⇒
        subscriptionServices.removeByCollectionAndUser(collection, user).transact(T)
    }
  }

  object userInterpreter extends (User.Ops ~> M) {

    private[this] val userServices: UserServices = UserServices.services

    def apply[A](fa: User.Ops[A]) = fa match {
      case User.Add(email, apiKey, sessionToken) ⇒
        userServices.addUser[domain.User](email, apiKey, sessionToken).transact(T)
      case User.AddInstallation(user, deviceToken, androidId) ⇒
        userServices.createInstallation[domain.Installation](user, deviceToken, androidId).transact(T)
      case User.GetByEmail(email) ⇒
        userServices.getUserByEmail(email).transact(T)
      case User.GetBySessionToken(sessionToken) ⇒
        userServices.getUserBySessionToken(sessionToken).transact(T)
      case User.GetInstallationByUserAndAndroidId(user, androidId) ⇒
        userServices.getInstallationByUserAndAndroidId(user, androidId).transact(T)
      case User.GetSubscribedInstallationByCollection(collectionPublicId) ⇒
        userServices.getSubscribedInstallationByCollection(collectionPublicId).transact(T)
      case User.UpdateInstallation(user, deviceToken, androidId) ⇒
        userServices.updateInstallation[domain.Installation](user, deviceToken, androidId).transact(T)
    }
  }
}

trait TaskInstances {
  implicit val taskMonad: Monad[Task] with ApplicativeError[Task, Throwable] with RecursiveTailRecM[Task] =
    new Monad[Task] with ApplicativeError[Task, Throwable] with RecursiveTailRecM[Task] {

      def pure[A](x: A): Task[A] = Task.delay(x)

      override def map[A, B](fa: Task[A])(f: A ⇒ B): Task[B] =
        fa map f

      override def flatMap[A, B](fa: Task[A])(f: A ⇒ Task[B]): Task[B] =
        fa flatMap f

      override def raiseError[A](e: Throwable): Task[A] =
        Task.fail(e)

      override def handleErrorWith[A](fa: Task[A])(f: Throwable ⇒ Task[A]): Task[A] =
        fa.handleWith({ case x ⇒ f(x) })

      override def tailRecM[A, B](a: A)(f: (A) ⇒ Task[Either[A, B]]): Task[B] =
        flatMap(f(a)) {
          case Right(b) ⇒ pure(b)
          case Left(nextA) ⇒ tailRecM(nextA)(f)
        }
    }
}

object Interpreters extends TaskInstances {

  val taskInterpreters = new Interpreters[Task] {
    override val task2M: (Task ~> Task) = new (Task ~> Task) {
      override def apply[A](fa: Task[A]): Task[A] = fa
    }
  }
}
