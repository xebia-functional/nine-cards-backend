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

import org.scalacheck.{ Arbitrary, Gen }
import org.specs2._
import org.specs2.matcher.{ DisjunctionMatchers, MatchResult }
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scalaz.std.iterable._

trait DatabaseContext extends PersistenceDatabaseContext {

  object WithEmptyTable {

    def apply[A](check: ⇒ MatchResult[A]) = {
      deleteItems(deleteAllSql).transactAndRun
      check
    }
  }

  object WithData {

    def apply[A](data: List[PersistenceItemData])(check: ⇒ MatchResult[A]) = {
      {
        for {
          _ ← deleteItems(deleteAllSql)
          _ ← insertItems(insertSql, data)
        } yield Unit
      }.transactAndRun

      check
    }

    def apply[A](data: PersistenceItemData)(check: Long ⇒ MatchResult[A]) = {
      val id = {
        for {
          _ ← deleteItems(deleteAllSql)
          id ← insertItem(insertSql, data)
        } yield id
      }.transactAndRun

      check(id)
    }
  }

  implicit val arbPersistenceItem: Arbitrary[PersistenceItem] = Arbitrary {
    for {
      id ← Gen.posNum[Long]
      name ← Gen.alphaStr
      status ← Gen.oneOf(true, false)
    } yield PersistenceItem(id = id, name = name, active = status)
  }

  implicit val arbPersistenceItemData: Arbitrary[PersistenceItemData] = Arbitrary {
    for {
      name ← Gen.alphaStr
      status ← Gen.oneOf(true, false)
    } yield PersistenceItemData(name = name, active = status)
  }

  implicit val arbPersistenceItemDataList: Arbitrary[List[PersistenceItemData]] = Arbitrary {
    for {
      size ← Gen.chooseNum(2, 100)
      items ← Gen.listOfN(size, arbPersistenceItemData.arbitrary)
    } yield items
  }
}

class PersistenceSpec
  extends Specification
  with DatabaseContext
  with DisjunctionMatchers
  with ScalaCheck
  with BeforeAfterAll {

  def beforeAll = runDDLQuery(createTableSql).transactAndRun

  def afterAll = runDDLQuery(dropTableSql).transactAndRun

  sequential

  "fetchList (SQL without parameters)" should {
    "return an empty list if the table is empty" in {
      prop { i: Int ⇒

        WithEmptyTable {
          persistence
            .fetchList(sql = fetchAllSql)
            .transactAndRun must beEmpty
        }
      }
    }
    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒

          WithData(data) {
            persistence
              .fetchList(sql = fetchAllSql)
              .transactAndRun must haveSize(data.size)
          }
        }
      }
    "return a list of PersistenceItem if there are some elements in the table " +
      "that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val namesWithStatus = data map (_.copy(active = false))

          WithData(namesWithStatus) {
            persistence
              .fetchList(sql = fetchAllActiveSql)
              .transactAndRun must beEmpty
          }
        }
      }
  }

  "fetchList" should {
    "return an empty list if the table is empty" in {
      prop { status: Boolean ⇒

        WithEmptyTable {
          persistence
            .fetchList(fetchByStatusSql, status)
            .transactAndRun must beEmpty
        }
      }
    }
    "return a list of PersistenceItem if there are some elements in the table that meet the criteria" in {
      prop { data: List[PersistenceItemData] ⇒

        val activeItems = data map (_.copy(active = true))

        WithData(activeItems) {
          val list = persistence.fetchList(sql = fetchByStatusSql, values = true).transactAndRun

          list must haveSize(data.size)
          list must contain { item: PersistenceItem ⇒ item.active must beTrue }.forall
        }
      }
    }
    "return an empty list if there aren't any elements in the table that meet the criteria" in {
      prop { data: List[PersistenceItemData] ⇒

        val activeItems = data map (_.copy(active = true))

        WithData(activeItems) {
          persistence
            .fetchList(sql = fetchByStatusSql, values = false)
            .transactAndRun must beEmpty
        }
      }
    }
  }

  "fetchOption" should {
    "return None if the table is empty" in {
      prop { status: Boolean ⇒

        WithEmptyTable {
          persistence
            .fetchOption(fetchByStatusSql, status)
            .transactAndRun must beEmpty
        }
      }
    }
    "return a PersistenceItem if there is an element in the table that meets the criteria" in {
      prop { data: PersistenceItemData ⇒

        WithData(data) { id ⇒
          val persistenceItem = persistence.fetchOption(
            sql    = fetchByIdAndStatusSql,
            values = (id, data.active)
          ).transactAndRun

          persistenceItem must beSome[PersistenceItem].which {
            item ⇒
              item.name must_== data.name
              item.active must_== data.active
          }
        }
      }
    }
    "return None if there isn't any element in the table that meets the criteria" in {
      prop { data: PersistenceItemData ⇒

        WithData(data) { id ⇒
          persistence
            .fetchOption(fetchByIdAndStatusSql, (id, !data.active))
            .transactAndRun must beNone
        }
      }
      "throw an exception if there are more than one element in the table that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val activeItems = data map (_.copy(active = true))

          WithData(activeItems) {
            persistence.fetchOption(
              sql    = fetchByStatusSql,
              values = true
            ).transactAndAttempt must be_-\/[Throwable]
          }
        }
      }
    }
  }

  "fetchUnique" should {
    "throw an exception if the table is empty" in {
      prop { id: Long ⇒

        WithEmptyTable {
          persistence.fetchUnique(
            sql    = fetchByIdSql,
            values = id
          ).transactAndAttempt must be_-\/[Throwable]
        }
      }
    }
    "return a PersistenceItem if there is an element in the table with the given id" in {
      prop { data: PersistenceItemData ⇒

        WithData(data) { id ⇒
          val item = persistence.fetchUnique(fetchByIdSql, id).transactAndRun

          item.name must_== data.name
          item.active must_== data.active
        }
      }
    }
    "throw an exception if there isn't any element in the table that meet the criteria" in {
      prop { data: PersistenceItemData ⇒

        WithData(data) { id ⇒
          persistence
            .fetchUnique(fetchByIdAndStatusSql, (id, !data.active))
            .transactAndAttempt must be_-\/[Throwable]
        }
      }
    }
    "throw an exception if there are more than one element in the table that meet the criteria" in {
      prop { data: List[PersistenceItemData] ⇒
        val activeItems = data map (_.copy(active = true))

        WithData(activeItems) {
          persistence
            .fetchUnique(fetchByStatusSql, true)
            .transactAndAttempt must be_-\/[Throwable]
        }
      }
    }
  }

  "update (SQL without parameters)" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
        prop { i: Int ⇒

          WithEmptyTable {
            persistence.update(
              sql = updateAllSql
            ).transactAndRun must_== 0
          }
        }
      }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val activeItems = data map (_.copy(active = true))

          WithData(activeItems) {
            persistence
              .update(updateAllActiveSql)
              .transactAndRun must_== activeItems.size
          }
        }
      }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val inactiveItems = data map (_.copy(active = false))

          WithData(inactiveItems) {
            persistence
              .update(updateAllActiveSql)
              .transactAndRun must_== 0
          }
        }
      }
  }

  "update" should {
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if the table is empty" in {
        prop { active: Boolean ⇒

          WithEmptyTable {
            persistence.update(
              sql    = updateByStatusSql,
              values = (!active, active)
            ).transactAndRun must_== 0
          }
        }
      }
    "return the number of affected rows equals to 1 after updating a single item in the table " in {
      prop { data: PersistenceItemData ⇒

        WithData(data) { id ⇒
          val (affectedRows, item) = {
            for {
              affectedRows ← persistence.update(updateByIdSql, (data.name, !data.active, id))
              item ← getItem[Long, PersistenceItem](fetchByIdSql, id)
            } yield (affectedRows, item)

          }.transactAndRun

          affectedRows must_== 1

          item.name must_== data.name
          item.active must_== !data.active
        }
      }
    }
    "return the number of affected rows after updating items in the table " +
      "if there are some elements that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val activeItems = data map (_.copy(active = true))

          WithData(activeItems) {
            persistence
              .update(updateByStatusSql, (false, true))
              .transactAndRun must be_>(0)
          }
        }
      }
    "return the number of affected rows equals to 0 after updating items in the table " +
      "if there aren't any elements that meet the criteria" in {
        prop { data: List[PersistenceItemData] ⇒
          val inactiveItems = data map (_.copy(active = false))

          WithData(inactiveItems) {
            persistence
              .update(updateByStatusSql, (false, true))
              .transactAndRun must_== 0
          }
        }
      }
    "return the number of affected rows equals to 1 after inserting a new item in the table" in {
      prop { data: PersistenceItemData ⇒

        WithEmptyTable {
          persistence.update(
            sql    = insertSql,
            values = (data.name, data.active)
          ).transactAndRun must_== 1
        }
      }
    }
  }

  "updateWithGeneratedKeys" should {
    "insert a new PersistenceItem into the table" in {
      prop { data: PersistenceItemData ⇒

        WithEmptyTable {
          val item = persistence.updateWithGeneratedKeys(
            sql    = insertSql,
            fields = allFields,
            values = data
          ).transactAndRun

          item.name must_== data.name
          item.active must_== data.active
        }
      }
    }
  }

  "updateMany" should {
    "return the number of affected rows after inserting a batch of items in the table" in {
      prop { data: List[PersistenceItemData] ⇒

        WithEmptyTable {
          persistence.updateMany(
            sql    = insertSql,
            values = data
          ).transactAndRun must_== data.size
        }
      }
    }

    "return the number of affected rows equals to 0 after updating a batch of items " +
      "in the table if the table is empty" in {
        prop { data: List[PersistenceItem] ⇒

          WithEmptyTable {
            persistence.updateMany(
              sql    = updateByIdSql,
              values = data map (item ⇒ (item.name, item.active, item.id))
            ).transactAndRun must_== 0
          }
        }
      }
    "return the number of affected rows after updating a batch of items in the table " +
      "if the items exist" in {
        prop { data: List[PersistenceItemData] ⇒

          WithData(data) {
            val affectedRows = {
              for {
                result ← getItems[(String, Boolean, Long)](getAllSql)
                updateData = result map { case (name, active, id) ⇒ (name, !active, id) }
                affectedRows ← persistence.updateMany(
                  sql    = updateByIdSql,
                  values = updateData
                )
              } yield affectedRows
            }.transactAndRun

            affectedRows must_== data.size
          }
        }
      }
  }
}
