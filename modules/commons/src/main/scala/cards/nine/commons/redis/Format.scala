package cards.nine.commons.redis

import io.circe.Encoder

trait Format[A] extends (A ⇒ String)

object Format {
  def apply[A](print: A ⇒ String) = new Format[A] {
    def apply(a: A): String = print(a)
  }

  def apply[A](encoder: Encoder[A]): Format[A] = new Format[A] {
    def apply(a: A): String = encoder(a).noSpaces
  }

}

