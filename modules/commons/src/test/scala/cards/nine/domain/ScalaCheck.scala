package cards.nine.domain

import cards.nine.domain.account.Email
import cards.nine.domain.analytics.DateRange
import cards.nine.domain.application.{ Category, Moment, Package, PriceFilter }
import com.fortysevendeg.scalacheck.datetime.instances.joda._
import com.fortysevendeg.scalacheck.datetime.GenDateTime.genDateTimeWithinRange
import enumeratum.{ Enum, EnumEntry }
import org.joda.time.{ DateTime, Period }
import org.scalacheck.{ Arbitrary, Gen }

object ScalaCheck {

  import Gen._

  def arbEnumeratum[C <: EnumEntry](e: Enum[C]): Arbitrary[C] = Arbitrary(Gen.oneOf(e.values))

  implicit val arbCategory: Arbitrary[Category] = arbEnumeratum[Category](Category)

  implicit val arbMoment: Arbitrary[Moment] = arbEnumeratum[Moment](Moment)

  implicit val arbPackage: Arbitrary[Package] = Arbitrary {
    Gen.listOfN(16, Gen.alphaNumChar) map (l ⇒ Package(l.mkString))
  }

  implicit val arbPriceFilter: Arbitrary[PriceFilter] = arbEnumeratum[PriceFilter](PriceFilter)

  implicit val arbDateRange: Arbitrary[DateRange] = Arbitrary {
    val rangeGenerator = genDateTimeWithinRange(DateTime.now, Period.months(1))
    for {
      date1 ← rangeGenerator
      date2 ← rangeGenerator
    } yield {
      if (date1.isBefore(date2))
        DateRange(date1, date2)
      else
        DateRange(date2, date1)
    }
  }

  def nonEmptyString(maxSize: Int) =
    Gen.resize(
      s = maxSize,
      g = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
    )

  def fixedLengthString(size: Int) = Gen.listOfN(size, Gen.alphaChar).map(_.mkString)

  def fixedLengthNumericString(size: Int) = Gen.listOfN(size, Gen.numChar).map(_.mkString)

  val emailGenerator: Gen[Email] = for {
    mailbox ← nonEmptyString(50)
    topLevelDomain ← nonEmptyString(45)
    domain ← fixedLengthString(3)
  } yield Email(s"$mailbox@$topLevelDomain.$domain")

  implicit val abEmail: Arbitrary[Email] = Arbitrary(emailGenerator)

  /** Generates an hexadecimal character */
  def hexChar: Gen[Char] = Gen.frequency(
    (10, Gen.numChar),
    (6, choose(97.toChar, 102.toChar))
  )

}

