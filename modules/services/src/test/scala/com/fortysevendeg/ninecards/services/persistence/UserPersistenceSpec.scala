package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.NineCardsGenEntities._
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices._
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach

class UserPersistenceSpec
  extends Specification
    with BeforeEach
    with DatabaseScope
    with DisjunctionMatchers
    with ScalaCheck
    with NineCardsScalacheckGen {

  sequential

  override def before = {
    flywaydb.clean()
    flywaydb.migrate
  }

  def insertItem[A](
    sql: String,
    values: A)(implicit ev: Composite[A]): ConnectionIO[Long] =
    Update[A](sql).toUpdate0(values).withUniqueGeneratedKeys[Long]("id")

  "getUserBySessionToken" should {
    "return None if the table is empty" in {
      prop { (email: Email, sessionToken: SessionToken) =>

        val user = userPersistenceImpl.getUserBySessionToken(sessionToken.value).transact(trx).run

        user should beNone
      }
    }
    "return an user if there is an user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken) =>
        insertItem(User.Queries.insert, (email.value, sessionToken.value)).transact(trx).run

        val user = userPersistenceImpl.getUserBySessionToken(sessionToken.value).transact(trx).run

        user should beSome[User]
      }
    }
    "return None if there isn't any user with the given sessionToken in the database" in {
      prop { (email: Email, sessionToken: SessionToken) =>
        insertItem(User.Queries.insert, (email.value, sessionToken.value)).transact(trx).run

        val user = userPersistenceImpl.getUserBySessionToken(sessionToken.value.reverse).transact(trx).run

        user should beNone
      }
    }
  }
}
