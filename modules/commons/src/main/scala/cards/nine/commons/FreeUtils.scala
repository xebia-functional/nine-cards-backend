package cards.nine.commons

import cats.data.{ Xor, XorT }
import cats.free.Free

object FreeUtils {

  implicit class FreeXorOps[F[_], A, B](free: Free[F, A Xor B]) {
    def toXorT = XorT[Free[F, ?], A, B](free)
  }

  implicit class FreeOps[G[_], A](free: Free[G, A]) {
    def toXorTRight[B] = XorT.right[Free[G, ?], B, A](free)
  }

  implicit class ProductOps[A <: Any](a: A) {
    def toFree[F[_]] = Free.pure[F, A](a)
  }
}
