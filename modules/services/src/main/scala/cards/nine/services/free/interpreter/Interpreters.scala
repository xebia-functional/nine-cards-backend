package cards.nine.services.free.interpreter

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
