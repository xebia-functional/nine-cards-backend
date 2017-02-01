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
package cards.nine.commons.redis

import cards.nine.commons.catscalaz.ScalaFuture2Task
import scala.concurrent.ExecutionContext
import scredis.serialization.{ Reader, Writer }
import scalaz.concurrent.Task

class CacheWrapper[Key, Val](
  implicit
  format: Format[Key],
  writer: Writer[Val],
  reader: Reader[Option[Val]],
  ec: ExecutionContext
) {

  def get(key: Key): RedisOps[Option[Val]] =
    client ⇒ ScalaFuture2Task {
      client.get[Option[Val]](format(key)).map(_.flatten)
    }

  def mget(keys: List[Key]): RedisOps[List[Val]] =
    client ⇒ {
      if (keys.isEmpty)
        Task(Nil)
      else
        ScalaFuture2Task {
          client.mGet[Option[Val]](keys.map(format): _*).map(_.flatten.flatten)
        }
    }

  def put(entry: (Key, Val)): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      val (key, value) = entry
      client.set[Val](format(key), value).map(x ⇒ Unit)
    }

  def mput(entries: List[(Key, Val)]): RedisOps[Unit] =
    client ⇒ {
      if (entries.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.mSet[Val](entries.map({ case (k, v) ⇒ format(k) → v }).toMap)
      }
    }

  def delete(key: Key): RedisOps[Unit] =
    client ⇒ ScalaFuture2Task {
      client.del(key).map(x ⇒ Unit)
    }

  def delete(keys: List[Key]): RedisOps[Unit] =
    client ⇒ {
      if (keys.isEmpty)
        Task(Unit)
      else ScalaFuture2Task {
        client.del(keys map format: _*).map(x ⇒ Unit)
      }
    }

}
