package cards.nine.services.free.interpreter

import cards.nine.commons.TaskInstances
import cats._
import cards.nine.services.free.algebra._
import cards.nine.services.free.interpreter.analytics.{ Services ⇒ AnalyticsServices }
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.firebase.{ Services ⇒ FirebaseServices }
import cards.nine.services.free.interpreter.googleapi.{ Services ⇒ GoogleApiServices }
import cards.nine.services.free.interpreter.googleplay.{ Services ⇒ GooglePlayServices }
import cards.nine.services.free.interpreter.ranking.{ Services ⇒ RankingServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import cards.nine.services.persistence.CustomComposite._
import cards.nine.services.persistence.DatabaseTransactor._
import doobie.imports._

import scalaz.concurrent.Task

abstract class Interpreters[M[_]](implicit A: ApplicativeError[M, Throwable], T: Transactor[M]) {

  val task2M: (Task ~> M)

  val connectionIO2M = new (ConnectionIO ~> M) {
    def apply[A](fa: ConnectionIO[A]): M[A] = fa.transact(T)
  }

  lazy val analyticsInterpreter: (GoogleAnalytics.Ops ~> M) = AnalyticsServices.services.andThen(task2M)

  val collectionInterpreter: (SharedCollection.Ops ~> M) = CollectionServices.services.andThen(connectionIO2M)

  val countryInterpreter: (Country.Ops ~> M) = CountryServices.services.andThen(connectionIO2M)

  lazy val firebaseInterpreter: (Firebase.Ops ~> M) = FirebaseServices.services.andThen(task2M)

  lazy val googleApiInterpreter: (GoogleApi.Ops ~> M) = GoogleApiServices.services.andThen(task2M)

  lazy val googlePlayInterpreter: (GooglePlay.Ops ~> M) = GooglePlayServices.services.andThen(task2M)

  val rankingInterpreter: (Ranking.Ops ~> M) = RankingServices.services.andThen(connectionIO2M)

  val subscriptionInterpreter: (Subscription.Ops ~> M) = SubscriptionServices.services.andThen(connectionIO2M)

  val userInterpreter: (User.Ops ~> M) = UserServices.services.andThen(connectionIO2M)
}

object Interpreters extends TaskInstances {

  val taskInterpreters = new Interpreters[Task] {
    override val task2M: (Task ~> Task) = new (Task ~> Task) {
      override def apply[A](fa: Task[A]): Task[A] = fa
    }
  }
}
