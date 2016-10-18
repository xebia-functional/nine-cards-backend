package cards.nine.domain.application

/**
  * A Package is the unique identifier of an android App.
  * It is a dot-separated sequence of lowercase segments, much like Java packages.
  */
case class Package(value: String) extends AnyVal

/**
  * A FullCard contains all the information about an existing Android App
  * managed by the NineCards Backend application.
  */
case class FullCard(
  packageName: Package,
  title: String,
  categories: List[String],
  downloads: String,
  free: Boolean,
  icon: String,
  screenshots: List[String],
  stars: Double
)

/**
  * A FullCardList carries the information known in the backend about a set of packages.
  * The `cards` field contains the FullCard for those packages for which one is available.
  * The `missing` field contains the name of those packages for which there is none.a
  */
case class FullCardList(
  missing: List[Package],
  cards: List[FullCard]
)