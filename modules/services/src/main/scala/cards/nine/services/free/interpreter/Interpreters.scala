package cards.nine.services.free.interpreter

import akka.actor.ActorSystem
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.commons.catscalaz.TaskInstances
import cards.nine.commons.config.Domain.{ GoogleAnalyticsConfiguration, GoogleApiConfiguration, GoogleFirebaseConfiguration }
import cards.nine.commons.redis.{ RedisClient, RedisOps }
import cats._
import cards.nine.services.free.algebra._
import cards.nine.services.free.interpreter.analytics.{ Services ⇒ AnalyticsServices }
import cards.nine.services.free.interpreter.collection.{ Services ⇒ CollectionServices }
import cards.nine.services.free.interpreter.country.{ Services ⇒ CountryServices }
import cards.nine.services.free.interpreter.firebase.{ Services ⇒ FirebaseServices }
import cards.nine.services.free.interpreter.googleapi.{ Services ⇒ GoogleApiServices }
import cards.nine.services.free.interpreter.googleoauth.{ Services ⇒ GoogleOAuthServices }
import cards.nine.services.free.interpreter.googleplay.{ Services ⇒ GooglePlayServices }
import cards.nine.services.free.interpreter.ranking.{ Services ⇒ RankingServices }
import cards.nine.services.free.interpreter.subscription.{ Services ⇒ SubscriptionServices }
import cards.nine.services.free.interpreter.user.{ Services ⇒ UserServices }
import cards.nine.services.persistence.DatabaseTransactor._
import doobie.contrib.postgresql.pgtypes._
import doobie.imports._
import scalaz.concurrent.Task
import scredis.{ Client ⇒ ScredisClient }

abstract class Interpreters[M[_]]()(implicit A: ApplicativeError[M, Throwable], T: Transactor[M]) {

  implicit val system: ActorSystem = ActorSystem("cards-nine-services-redis")

  val task2M: (Task ~> M)

  val redisClient: RedisClient = ScredisClient(
    host        = nineCardsConfiguration.redis.host,
    port        = nineCardsConfiguration.redis.port,
    passwordOpt = nineCardsConfiguration.redis.secret
  )

  implicit val fanalyticsConfig: GoogleAnalyticsConfiguration = nineCardsConfiguration.google.analytics
  implicit val firebaseConfig: GoogleFirebaseConfiguration = nineCardsConfiguration.google.firebase
  implicit val googleApiConfig: GoogleApiConfiguration = nineCardsConfiguration.google.api

  val toTask = new (RedisOps ~> Task) {

    override def apply[A](fa: RedisOps[A]): Task[A] = fa(redisClient)
  }

  val connectionIO2M = new (ConnectionIO ~> M) {
    def apply[A](fa: ConnectionIO[A]): M[A] = fa.transact(T)
  }

  lazy val analyticsInterpreter: (GoogleAnalytics.Ops ~> M) = AnalyticsServices.services.andThen(task2M)

  val collectionInterpreter: (SharedCollection.Ops ~> M) = CollectionServices.services.andThen(connectionIO2M)

  val countryInterpreter: (Country.Ops ~> M) = CountryServices.services.andThen(connectionIO2M)

  lazy val firebaseInterpreter: (Firebase.Ops ~> M) = FirebaseServices.services.andThen(task2M)

  lazy val googleApiInterpreter: (GoogleApi.Ops ~> M) = GoogleApiServices.services.andThen(task2M)

  lazy val googleOAuthInterpreter: (GoogleOAuth.Ops ~> M) = GoogleOAuthServices.andThen(task2M)

  lazy val googlePlayInterpreter: (GooglePlay.Ops ~> M) = GooglePlayServices.services.andThen(task2M)

  lazy val rankingInterpreter: (Ranking.Ops ~> M) = RankingServices.services().andThen(toTask).andThen(task2M)

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
