package com.fortysevendeg.ninecards.services.persistence

object PersistenceExceptions {

  case class PersistenceException(message: String, cause: Option[Throwable] = None)
    extends Throwable(message) {
    cause foreach initCause
  }

}