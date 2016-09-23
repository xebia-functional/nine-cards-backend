package cards.nine.services

import doobie.imports.ConnectionIO

import scalaz.Scalaz._

package object persistence {

  implicit def toConnectionIO[T](t: T): ConnectionIO[T] = t.point[ConnectionIO]
}
