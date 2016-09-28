package cards.nine.googleplay

import cats.{ApplicativeError, Monad, RecursiveTailRecM}
import cats.data.Xor
import scalaz.concurrent.Task

object extracats {

  def splitXors[L,R](xors: List[Xor[L,R]]) : (List[L], List[R]) = {
    def splitXor[L,R](xor: Xor[L,R], ps: (List[L], List[R])) : (List[L], List[R]) = xor match {
      case Xor.Left(l)  => (l::ps._1, ps._2)
      case Xor.Right(r) => (ps._1, r::ps._2)
    }
    xors.foldRight[(List[L],List[R])]( (Nil,Nil) ) (splitXor)
  }

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
        defaultTailRecM(a)(f)
    }

  class XorTaskOrComposer[A,E,B](
    leftFunction: (A => Task[Xor[E,B]]),
    rightFunction: (A => Task[Xor[E,B]])
  ) extends (A => Task[Xor[E,B]]) {

    def apply(a: A) : Task[Xor[E,B]] = {
      // Currently this results in twoR being called twice
      // if oneR fails and twoR returns Xor.left
      val leftResult: Task[Xor[E,B]] = leftFunction(a)
      lazy val rightResult: Task[Xor[E,B]] = rightFunction(a)
      val orResult = leftResult.or(rightResult)
      orResult.flatMap {
        case Xor.Left(_) => rightResult
        case res => Task.now(res)
      }
    }
  }

}
