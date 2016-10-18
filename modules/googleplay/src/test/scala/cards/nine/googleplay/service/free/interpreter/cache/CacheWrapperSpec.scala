package cards.nine.googleplay.service.free.interpreter.cache

import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.{ BeforeAfterAll, BeforeEach }
import redis.embedded.RedisServer

trait RedisTestDomain {

  case class TestCacheKey(key: String)

  case class TestCacheVal(value: String)

  case class TestCacheEntryBatch(entries: List[(TestCacheKey, TestCacheVal)])

  def generateFixedLengthString(size: Int) = Gen.listOfN(16, Gen.alphaNumChar) map (_.mkString)

  implicit val arbTestCacheKey: Arbitrary[TestCacheKey] = Arbitrary(generateFixedLengthString(16) map TestCacheKey)
  implicit val arbTestCacheVal: Arbitrary[TestCacheVal] = Arbitrary(generateFixedLengthString(16) map TestCacheVal)
  implicit val genTestCacheEntry: Gen[(TestCacheKey, TestCacheVal)] =
    for {
      key ← arbTestCacheKey.arbitrary
      value ← arbTestCacheVal.arbitrary
    } yield (key, value)
  implicit val arbTestCacheEntryBatch: Arbitrary[TestCacheEntryBatch] = Arbitrary(Gen.listOfN(20, genTestCacheEntry) map TestCacheEntryBatch)

  implicit val cacheKeyDecoder: Decoder[TestCacheKey] = deriveDecoder[TestCacheKey]
  implicit val cacheKeyEncoder: Encoder[TestCacheKey] = deriveEncoder[TestCacheKey]
  implicit val cacheValDecoder: Decoder[TestCacheVal] = deriveDecoder[TestCacheVal]
  implicit val cacheValEncoder: Encoder[TestCacheVal] = deriveEncoder[TestCacheVal]

  implicit val keyParse: Parse[Option[TestCacheKey]] =
    Parse(bv ⇒ decode[TestCacheKey](Parse.Implicits.parseString(bv)).toOption)

  implicit val valParse: Parse[Option[TestCacheVal]] =
    Parse(bv ⇒ decode[TestCacheVal](Parse.Implicits.parseString(bv)).toOption)

  implicit val keyAndValFormat: Format =
    Format {
      case key: TestCacheKey ⇒ cacheKeyEncoder(key).noSpaces
      case value: TestCacheVal ⇒ cacheValEncoder(value).noSpaces
    }
}

trait RedisScope extends RedisTestDomain {

  lazy val redisServer: RedisServer = new RedisServer()
  lazy val redisClient: RedisClient = new RedisClient(host = "localhost", port = redisServer.getPort)
  lazy val wrapper = CacheWrapper.cacheWrapper[TestCacheKey, TestCacheVal](redisClient)

  def findEntry(key: TestCacheKey): Option[TestCacheVal] = redisClient.get[Option[TestCacheVal]](key).flatten

  def findEntries(keys: List[TestCacheKey]): List[TestCacheVal] = keys match {
    case head :: tail ⇒
      redisClient.mget[Option[TestCacheVal]](head, tail: _*)
        .getOrElse(Nil)
        .flatMap(_.flatten.toList)
    case _ ⇒ Nil
  }

  def getAllKeys: List[TestCacheKey] =
    redisClient
      .keys[Option[TestCacheKey]](JsonPattern.print(PObject(List(PString("key") → PStar))))
      .getOrElse(Nil)
      .flatMap(_.flatten.toList)
}

class CacheWrapperSpec
  extends Specification
  with ScalaCheck
  with BeforeAfterAll
  with BeforeEach
  with RedisScope {

  override def afterAll(): Unit = redisServer.stop()

  override def beforeAll(): Unit = redisServer.start()

  override protected def before: Any = redisClient.flushall

  object WithCachedData {

    def apply[K, V, B](key: K, value: V)(check: ⇒ MatchResult[B]) = {
      redisClient.flushall
      redisClient.set(key, value)
      check
    }

    def apply[K, V, B](data: List[(K, V)])(check: ⇒ MatchResult[B]) = {
      redisClient.flushall
      redisClient.mset(data: _*)
      check
    }
  }

  sequential

  "delete" should {
    "do nothing if there is no entry for the given key" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒

        WithCachedData(key, value) {
          wrapper.delete(TestCacheKey(key.key.reverse))

          val existingValue = findEntry(key)

          existingValue must beSome[TestCacheVal](value)
        }
      }
    }
    "delete an entry if it exists for the given key" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒

        WithCachedData(key, value) {
          wrapper.delete(key)

          val existingValue = findEntry(key)

          existingValue must beNone
        }
      }
    }
  }

  "delete (multiple keys)" should {
    "do nothing if an empty list is provided" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (keys, values) = batch.entries.unzip

        WithCachedData(batch.entries) {
          wrapper.delete(Nil)

          val existingValues = findEntries(keys)

          existingValues must containTheSameElementsAs(values)
        }
      }
    }
    "delete those keys that have an entry in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (existing, nonExisting) = batch.entries.splitAt(batch.entries.size / 2)
        val (deleted, permanent) = existing.splitAt(batch.entries.size / 2)
        val (deletedKeys, _) = deleted.unzip
        val (permanentKeys, permanentValues) = permanent.unzip
        val (nonExistingKeys, _) = nonExisting.unzip
        val allKeys = deletedKeys ++ permanentKeys ++ nonExistingKeys

        WithCachedData(existing) {

          wrapper.delete(deletedKeys)

          val deletedValues = findEntries(deletedKeys)
          val existingValues = findEntries(allKeys)

          existingValues must containTheSameElementsAs(permanentValues)
          deletedValues must beEmpty
        }
      }
    }
  }

  "findFirst" should {
    "return None if there is no entry for the given keys" in {
      prop { (key1: TestCacheKey, value1: TestCacheVal, key2: TestCacheKey, key3: TestCacheKey) ⇒

        WithCachedData(key1, value1) {
          val foundValue = wrapper.findFirst(List(key2, key3))

          foundValue must beNone
        }
      }
    }
    "return the first element of the list if there are entries for all the keys" in {
      prop { (key1: TestCacheKey, value1: TestCacheVal, key2: TestCacheKey, value2: TestCacheVal) ⇒

        WithCachedData(List((key1, value1), (key2, value2))) {
          val foundValue = wrapper.findFirst(List(key1, key2))

          foundValue must beSome[TestCacheVal](value1)
        }
      }
    }

    "return the first existing element of the list if there are no entries for all the keys " in {
      prop { (key1: TestCacheKey, value1: TestCacheVal, key2: TestCacheKey, value2: TestCacheVal) ⇒

        WithCachedData(List((key2, value2))) {
          val foundValue = wrapper.findFirst(List(key1, key2))

          foundValue must beSome[TestCacheVal](value2)
        }
      }
    }
  }

  "get" should {
    "return None if there is no entry for the given key" in {
      prop { key: TestCacheKey ⇒
        val result = wrapper.get(key)

        result must beEmpty
      }
    }
    "return a value if there is an entry for the given key" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒
        WithCachedData(key, value) {
          wrapper.get(key) must beSome[TestCacheVal](value)
        }
      }
    }
  }

  "mget" should {
    "return an empty list if an empty list of keys is given" in {
      prop { i: Int ⇒
        val result = wrapper.mget(Nil)

        result must beEmpty
      }
    }
    "return a list of values for those keys that have an entry in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (found, notFound) = batch.entries.splitAt(batch.entries.size / 2)
        val (keys, _) = batch.entries.unzip
        val (_, foundValues) = found.unzip
        val (_, notFoundValues) = notFound.unzip

        WithCachedData(found) {
          val result = wrapper.mget(keys)

          result must containTheSameElementsAs(foundValues) and
            not(containTheSameElementsAs(notFoundValues))
        }
      }
    }
  }

  "put" should {
    "create a new entry into the cache if the given key doesn't exist" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒
        wrapper.put((key, value))

        val insertedValue = findEntry(key)

        insertedValue must beSome[TestCacheVal](value)
      }
    }
    "update an existing entry if the given key exists in cache" in {
      prop { (key: TestCacheKey, value: TestCacheVal, newValue: TestCacheVal) ⇒

        WithCachedData(key, value) {
          wrapper.put((key, newValue))

          val insertedValue = findEntry(key)

          insertedValue must beSome[TestCacheVal](newValue)
        }
      }
    }
  }

  "mput" should {
    "do nothing if an empty list is provided" in {
      prop { i: Int ⇒
        wrapper.mput(Nil)

        getAllKeys must beEmpty
      }
    }
    "create a new entry for each key if these keys don't exist in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (keys, values) = batch.entries.unzip

        redisClient.flushall

        wrapper.mput(batch.entries)

        val insertedValues = findEntries(keys)

        insertedValues must containTheSameElementsAs(values)
      }
    }
    "create or update an existing entry depending on the existence of keys in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (existing, nonExisting) = batch.entries.splitAt(batch.entries.size / 2)
        val (existingKeys, existingValues) = existing.unzip
        val (nonExistingKeys, nonExistingValues) = nonExisting.unzip

        WithCachedData(existing) {

          val newExistingValues = existingValues map (v ⇒ TestCacheVal(v.value.reverse))

          wrapper.mput(nonExisting ++ existingKeys.zip(newExistingValues))

          val insertedValues = findEntries(nonExistingKeys)
          val updatedValues = findEntries(existingKeys)

          insertedValues must containTheSameElementsAs(nonExistingValues)
          updatedValues must containTheSameElementsAs(newExistingValues)
          updatedValues must not(containTheSameElementsAs(existingValues))
        }
      }
    }
  }

  "matchKeys" should {
    "return an empty list if there are no keys for the given pattern" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒

        val pattern = PObject(List(PString("key") → PStar))

        val foundKeys = wrapper.matchKeys(pattern)

        foundKeys must beEmpty
      }
    }
    "return a list with the keys that match the given pattern" in {
      prop { batch: TestCacheEntryBatch ⇒

        val pattern = PObject(List(PString("key") → PString("*a*")))

        WithCachedData(batch.entries) {
          val foundKeys = wrapper.matchKeys(pattern)

          foundKeys must contain { k: TestCacheKey ⇒ k.key must contain("a") }.forall
        }
      }
    }
  }
}
