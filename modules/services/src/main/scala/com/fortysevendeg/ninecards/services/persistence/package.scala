package com.fortysevendeg.ninecards.services

import doobie.contrib.h2.h2transactor.H2Transactor

import scalaz.concurrent.Task

package object persistence {

  def transactor[F[_]] = H2Transactor[Task]("jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1", "sa", "").run

}
