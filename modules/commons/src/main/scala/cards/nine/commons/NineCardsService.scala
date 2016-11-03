package cards.nine.commons

import cards.nine.commons.NineCardsErrors.NineCardsError
import cats.data.EitherT
import cats.free._
import cats.syntax.either._

object NineCardsService {

  type NineCardsService[F[_], A] = EitherT[Free[F, ?], NineCardsError, A]

  type Result[A] = NineCardsError Either A

  def apply[F[_], A](f: Free[F, Result[A]]): NineCardsService[F, A] = {
    EitherT[Free[F, ?], NineCardsError, A](f)
  }

  def apply[Ops[_], F[_], A](op: Ops[Result[A]])(implicit I: Ops :<: F): NineCardsService[F, A] =
    apply[F, A](Free.inject[Ops, F](op))

  def fromEither[F[_], A](e: Result[A]) = NineCardsService[F, A](Free.pure(e))

  def left[F[_], A](e: NineCardsError) = NineCardsService[F, A](Free.pure(Either.left(e)))

  def right[F[_], A](a: A): NineCardsService[F, A] = NineCardsService[F, A](Free.pure(Either.right(a)))
}
