package cards.nine.domain.application

import org.scalacheck.{ Arbitrary, Gen }

object ScalaCheck {

  val genPackage: Gen[Package] =
    Gen.nonEmptyListOf(Gen.alphaNumChar).map(chars ⇒ Package(chars.mkString(".")))

  implicit val arbPackage: Arbitrary[Package] = Arbitrary(genPackage)

}

