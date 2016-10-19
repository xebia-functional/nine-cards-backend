package cards.nine.domain.application

import org.scalacheck.{ Arbitrary, Gen }

object ScalaCheck {

  implicit val arbPackage: Arbitrary[Package] = Arbitrary {
    Gen.listOfN(16, Gen.alphaNumChar) map (l ⇒ Package(l.mkString))
  }

}

