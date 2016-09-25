package cards.nine.services.common

import cats.data.{ Xor, XorT }
import cats.free.Free

object FreeUtils {

  implicit class CatsFreeOps[F[_], A, T](c: Free[F, T Xor A]) {
    def toXorT = XorT[cats.free.Free[F, ?], T, A](c)
  }

  implicit class ProductOps[A <: Any](a: A) {
    def toFree[F[_]] = Free.pure[F, A](a)
  }
}
