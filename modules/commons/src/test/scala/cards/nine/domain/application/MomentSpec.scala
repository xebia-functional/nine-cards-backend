package cards.nine.domain.application

import cards.nine.domain.ScalaCheck._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class MomentSpec
  extends Specification
  with ScalaCheck {

  "isMoment" should {
    "return true if a moment name is passed" in {
      prop { moment: Moment ⇒
        Moment.isMoment(moment.entryName) must beTrue
      }
    }

    "return true if a widget moment name is passed" in {
      prop { moment: Moment ⇒
        Moment.isMoment(s"${Moment.widgetMomentPrefix}${moment.entryName}") must beTrue
      }
    }

    "return true if a widget moment name is passed" in {
      prop { category: Category ⇒
        Moment.isMoment(category.entryName) must beFalse
      }
    }
  }
}
