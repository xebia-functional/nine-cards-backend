package com.fortysevendeg.ninecards.processes

object ProcessesExceptions {

  case class SharedCollectionNotFoundException(
    message: String,
    cause: Option[Throwable] = None) extends Throwable(message) {
    cause map initCause
  }

}
