package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain.{ Category, PackageName }
import doobie.imports.Composite

object CustomComposite {

  private[this] val string: Composite[String] = implicitly[Composite[String]]

  implicit val packageName: Composite[PackageName] =
    string.xmap[PackageName](PackageName.apply, _.name)

  implicit val categoryEntry: Composite[Category] =
    string.xmap(Category.withName, _.entryName)

}