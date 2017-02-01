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
package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import org.specs2.mutable.Specification
import org.joda.time.{ DateTime, DateTimeZone }
import io.circe.{ Decoder, Encoder }
import io.circe.parser._
import io.circe.syntax._

class FormatsSpec extends Specification {

  import Formats._

  val date: DateTime = new DateTime(2016, 7, 23, 12, 0, 14, DateTimeZone.UTC)
  val dateStr: String = "160723120014000"

  val fortysevenDeg = "com.fortyseven.deg"
  val packageName = Package(fortysevenDeg)

  def checkKeyFormatParse(key: CacheKey, keyStr: String) = {
    val keyType = key.keyType.entryName

    s"The Coders for a CacheKey of type $keyType" should {
      "format a $keyType key into a full string " in (keyFormat(key) must_=== keyStr)
      "parse a string into a $keyType Key" in (parseKey(keyStr) must_=== Some(key))
    }
  }

  checkKeyFormatParse(CacheKey.resolved(packageName), s"com.fortyseven.deg:Resolved")

  checkKeyFormatParse(CacheKey.error(packageName), s"com.fortyseven.deg:Error")

  val fullCard = FullCard(
    packageName = Package(fortysevenDeg),
    title       = "47 Degrees",
    categories  = List("Consulting"),
    downloads   = "42",
    free        = true,
    icon        = "http://icon",
    screenshots = List(),
    stars       = 3.14
  )

  val fullCardJsonStr = """
  | { "packageName" : "com.fortyseven.deg",
  |   "title" : "47_Degrees",
  |   "categories" : [ "Consulting" ],
  |   "downloads" : "42",
  |   "free" : true,
  |   "icon" : "http://icon",
  |   "screenshots" : [],
  |   "stars" : 3.14  }
  """.stripMargin.filter(_ > ' ').replaceAll("_", " ").trim

  def checkCoders[A](typeName: String, obj: A, jsonStr: String)(implicit dec: Decoder[A], enc: Encoder[A]) = {
    s"The Coders for $typeName" should {

      s"encode a $typeName into a full string " in (obj.asJson.noSpaces must_=== jsonStr)
      s"parse a string into a $typeName" in (decode[A](jsonStr) must beRight(obj))

    }
  }

  checkCoders[FullCard]("A Full Card", fullCard, fullCardJsonStr)

  checkCoders[CacheVal]("A Cache Value with a FullCard", CacheVal(Some(fullCard)), fullCardJsonStr)

  checkCoders[CacheVal]("An empty Cache Value", CacheVal(None), """ null """.trim)

}