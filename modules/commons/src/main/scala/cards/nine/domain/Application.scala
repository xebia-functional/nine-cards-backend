package cards.nine.domain.application

/**
  * A Package is the unique identifier of an android App.
  * It is a dot-separated sequence of lowercase segments, much like Java packages.
  */
case class Package(value: String) extends AnyVal

