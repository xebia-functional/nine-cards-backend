package cards.nine.processes

object ProcessesExceptions {

  case class SharedCollectionNotFoundException(
    message: String,
    cause: Option[Throwable] = None
  ) extends Throwable(message) {
    cause foreach initCause
  }

}
