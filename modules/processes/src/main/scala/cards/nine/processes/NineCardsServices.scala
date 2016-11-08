package cards.nine.processes

import cards.nine.services.free.algebra._
import cards.nine.services.free.interpreter.Interpreters
import cards.nine.services.free.interpreter.Interpreters._
import cats.data.Coproduct
import cats.{ ApplicativeError, Monad, RecursiveTailRecM, ~> }

import scalaz.concurrent.Task

object NineCardsServices {

  implicit val taskMonadInstance: Monad[Task] with ApplicativeError[Task, Throwable] with RecursiveTailRecM[Task] = taskMonad

  type NineCardsServicesC08[A] = Coproduct[GoogleOAuth.Ops, User.Ops, A]
  type NineCardsServicesC07[A] = Coproduct[Subscription.Ops, NineCardsServicesC08, A]
  type NineCardsServicesC06[A] = Coproduct[SharedCollection.Ops, NineCardsServicesC07, A]
  type NineCardsServicesC05[A] = Coproduct[Ranking.Ops, NineCardsServicesC06, A]
  type NineCardsServicesC04[A] = Coproduct[Country.Ops, NineCardsServicesC05, A]
  type NineCardsServicesC03[A] = Coproduct[GooglePlay.Ops, NineCardsServicesC04, A]
  type NineCardsServicesC02[A] = Coproduct[GoogleApi.Ops, NineCardsServicesC03, A]
  type NineCardsServicesC01[A] = Coproduct[GoogleAnalytics.Ops, NineCardsServicesC02, A]
  type NineCardsServices[A] = Coproduct[Firebase.Ops, NineCardsServicesC01, A]

  class NineCardsInterpreters[F[_]](int: Interpreters[F]) {

    val interpretersC08: NineCardsServicesC08 ~> F = int.googleOAuthInterpreter or int.userInterpreter

    val interpretersC07: NineCardsServicesC07 ~> F = int.subscriptionInterpreter or interpretersC08

    val interpretersC06: NineCardsServicesC06 ~> F = int.collectionInterpreter or interpretersC07

    val interpretersC05: NineCardsServicesC05 ~> F = int.rankingInterpreter or interpretersC06

    val interpretersC04: NineCardsServicesC04 ~> F = int.countryInterpreter or interpretersC05

    val interpretersC03: NineCardsServicesC03 ~> F = int.googlePlayInterpreter or interpretersC04

    val interpretersC02: NineCardsServicesC02 ~> F = int.googleApiInterpreter or interpretersC03

    val interpretersC01: NineCardsServicesC01 ~> F = int.analyticsInterpreter or interpretersC02

    val interpreters: NineCardsServices ~> F = int.firebaseInterpreter or interpretersC01
  }

  val prodNineCardsInterpreters = new NineCardsInterpreters(taskInterpreters)

  val prodInterpreters: NineCardsServices ~> Task = prodNineCardsInterpreters.interpreters
}