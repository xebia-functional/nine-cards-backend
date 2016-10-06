package cards.nine.googleplay.service.free.interpreter.cache

import cats.data.Xor
import cards.nine.googleplay.domain.{ Package, FullCard }
import org.specs2.mutable.Specification
import org.joda.time.{ DateTime, DateTimeZone }
import io.circe.{ Decoder, Encoder }
import io.circe.parser._
import io.circe.syntax._

class CirceCodersSpec extends Specification {

  import CirceCoders._

  def checkCoders[A](typeName: String, obj: A, jsonStr: String)(implicit dec: Decoder[A], enc: Encoder[A]) = {
    s"The Coders for $typeName" should {

      s"encode a $typeName into a full string " in (obj.asJson.noSpaces must_=== (jsonStr))
      s"parse a string into a $typeName" in (decode[A](jsonStr) must_=== (Xor.Right(obj)))

    }
  }

  val date: DateTime = new DateTime(2016, 7, 23, 12, 0, 14, DateTimeZone.UTC)
  val dateJsonStr = s""" "160723120014000" """.trim

  val fortysevenDeg = "com.fortyseven.deg"
  val packageName = Package(fortysevenDeg)

  val resolvedKey = CacheKey.resolved(packageName)
  val resolvedStr = """
    | { "package" : "com.fortyseven.deg",
    |   "keyType" : "Resolved",
    |   "date" : null }
    """.stripMargin.filter(_ > ' ').trim // to remove whitespace

  val errorKey = CacheKey.error(packageName, date)
  val errorJsonStr = """
    | { "package" : "com.fortyseven.deg",
    |   "keyType" : "Error",
    |   "date" : "160723120014000" }
  """.stripMargin.filter(_ > ' ').trim

  val fullCard = FullCard(
    packageName = fortysevenDeg,
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

  checkCoders[DateTime]("date in the cache keys", date, dateJsonStr)

  checkCoders[CacheKey]("Cache Keys for Resolved Packages", resolvedKey, resolvedStr)

  checkCoders[CacheKey]("Cache keys for an Error package", errorKey, errorJsonStr)

  checkCoders[FullCard]("A Full Card", fullCard, fullCardJsonStr)

  checkCoders[CacheVal]("A Cache Value with a FullCard", CacheVal(Some(fullCard)), fullCardJsonStr)

  checkCoders[CacheVal]("An empty Cache Value", CacheVal(None), """ null """.trim)

}