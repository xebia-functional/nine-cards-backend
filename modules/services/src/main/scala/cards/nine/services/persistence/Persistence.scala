/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.services.persistence

import doobie.imports.{ Composite, ConnectionIO, Query, Query0, Update, Update0 }
import shapeless.HNil
import scalaz.Foldable

class Persistence[K: Composite] {

  def generateQuery(sql: String): Query0[K] =
    Query[HNil, K](sql).toQuery0(HNil)

  class GenerateQuery[L] {
    def apply[A: Composite](sql: String, values: A)(implicit L: Composite[L]): Query0[L] =
      Query[A, L](sql).toQuery0(values)
  }

  def generateQuery = new GenerateQuery[K]

  def generateQueryFor[L] = new GenerateQuery[L]

  def generateUpdateWithGeneratedKeys[A: Composite](sql: String, values: A): Update0 =
    Update[A](sql).toUpdate0(values)

  def generateUpdate(sql: String): Update0 = Update0(sql, None)

  class FetchList[L] {
    def apply(sql: String)(implicit L: Composite[L]): ConnectionIO[List[L]] =
      Query[HNil, L](sql).toQuery0(HNil).to[List]

    def apply[A: Composite](sql: String, values: A)(implicit L: Composite[L]): ConnectionIO[List[L]] =
      Query[A, L](sql).to[List](values)
  }

  def fetchList = new FetchList[K]

  def fetchListAs[L] = new FetchList[L]

  def fetchOption[A: Composite](sql: String, values: A): ConnectionIO[Option[K]] =
    Query[A, K](sql).option(values)

  def fetchUnique[A: Composite](sql: String, values: A): ConnectionIO[K] =
    Query[A, K](sql).unique(values)

  def update(sql: String): ConnectionIO[Int] = Update[HNil](sql).run(HNil)

  def update[A: Composite](sql: String, values: A): ConnectionIO[Int] =
    Update[A](sql).run(values)

  def updateWithGeneratedKeys[A: Composite](sql: String, fields: List[String], values: A): ConnectionIO[K] =
    Update[A](sql).withUniqueGeneratedKeys[K](fields: _*)(values)

  def updateMany[F[_]: Foldable, A: Composite](sql: String, values: F[A]): ConnectionIO[Int] =
    Update[A](sql).updateMany(values)

}

object Persistence {

  implicit def persistence[K: Composite]: Persistence[K] = new Persistence
}
