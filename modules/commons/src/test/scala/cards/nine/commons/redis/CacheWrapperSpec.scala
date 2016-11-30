package cards.nine.commons.redis

import io.circe.{ Decoder, Encoder }
import io.circe.parser._
import io.circe.generic.semiauto._
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.specification.{ BeforeAfterAll, BeforeEach }
import redis.embedded.RedisServer
import scredis.Client
import scredis.serialization.{ Reader, Writer }
import scala.concurrent.{ Await, Future } // FIXME
import scala.concurrent.duration.Duration

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

  implicit val keyReader: Reader[Option[TestCacheKey]] = Readers.decoder(cacheKeyDecoder)
  implicit val valReader: Reader[Option[TestCacheVal]] = Readers.decoder(cacheValDecoder)

  implicit val valWriter: Writer[TestCacheVal] = Writers.encoder(cacheValEncoder)

  implicit val keyFormat: Format[TestCacheKey] = Format(cacheKeyEncoder)
}

trait RedisScope extends RedisTestDomain {

  import TestUtils.redisTestActorSystem

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val redisServer: RedisServer = new RedisServer()
  lazy val redisClient: Client = Client(host = "localhost", port = redisServer.getPort)
  lazy val wrapper = new CacheWrapper[TestCacheKey, TestCacheVal]()

  def await[A](fut: Future[A]): A = Await.result(fut, Duration.Inf)

  def run[A](ops: RedisOps[A]): A = ops(redisClient).unsafePerformSync

  def findEntry(key: TestCacheKey): Option[TestCacheVal] = await {
    redisClient.get[Option[TestCacheVal]](key).map(_.flatten)
  }

  def findEntries(keys: List[TestCacheKey]): List[TestCacheVal] = await {
    if (keys.isEmpty)
      Future(Nil)
    else
      for {
        opts ← redisClient.mGet[Option[TestCacheVal]]((keys map keyFormat): _*)
      } yield opts.flatten.flatten
  }

  def getAllKeys: List[TestCacheKey] = await {
    for {
      set ← redisClient.keys("*")
    } yield set.toList.flatMap(str ⇒ decode[TestCacheKey](str).toOption)
  }

}

class CacheWrapperSpec
  extends Specification
  with ScalaCheck
  with BeforeAfterAll
  with BeforeEach
  with RedisScope {

  override def afterAll(): Unit = redisServer.stop()

  override def beforeAll(): Unit = redisServer.start()

  override protected def before: Any = redisClient.flushAll

  object WithCachedData {

    def apply[K, V, B](key: K, value: V)(check: ⇒ MatchResult[B])(
      implicit
      fk: Format[K], wv: Writer[V]
    ) = {
      redisClient.flushAll
      redisClient.set[V](fk(key), value)
      check
    }

    def apply[K, V, B](data: List[(K, V)])(check: ⇒ MatchResult[B])(
      implicit
      fk: Format[K], wv: Writer[V]
    ) = {
      def pair(p: (K, V)): (String, V) = (fk(p._1), p._2)
      redisClient.flushAll
      redisClient.mSet(data.map(pair).toMap)
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
          run(wrapper.delete(key))

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
          run(wrapper.delete(Nil))

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

          run(wrapper.delete(deletedKeys))

          val deletedValues = findEntries(deletedKeys)
          val existingValues = findEntries(allKeys)

          existingValues must containTheSameElementsAs(permanentValues)
          deletedValues must beEmpty
        }
      }
    }
  }

  "get" should {
    "return None if there is no entry for the given key" in {
      prop { key: TestCacheKey ⇒
        run(wrapper.get(key)) must beEmpty
      }
    }
    "return a value if there is an entry for the given key" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒
        WithCachedData(key, value) {
          run(wrapper.get(key)) must beSome[TestCacheVal](value)
        }
      }
    }
  }

  "mget" should {
    "return an empty list if an empty list of keys is given" in {
      prop { i: Int ⇒
        run(wrapper.mget(Nil)) must beEmpty
      }
    }
    "return a list of values for those keys that have an entry in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (found, notFound) = batch.entries.splitAt(batch.entries.size / 2)
        val (keys, _) = batch.entries.unzip
        val (_, foundValues) = found.unzip
        val (_, notFoundValues) = notFound.unzip

        WithCachedData(found) {
          run(wrapper.mget(keys)) must containTheSameElementsAs(foundValues) and
            not(containTheSameElementsAs(notFoundValues))
        }
      }
    }
  }

  "put" should {
    "create a new entry into the cache if the given key doesn't exist" in {
      prop { (key: TestCacheKey, value: TestCacheVal) ⇒
        run(wrapper.put((key, value)))
        findEntry(key) must beSome[TestCacheVal](value)
      }
    }
    "update an existing entry if the given key exists in cache" in {
      prop { (key: TestCacheKey, value: TestCacheVal, newValue: TestCacheVal) ⇒

        WithCachedData(key, value) {
          run(wrapper.put((key, newValue)))
          findEntry(key) must beSome[TestCacheVal](newValue)
        }
      }
    }
  }

  "mput" should {
    "do nothing if an empty list is provided" in {
      prop { i: Int ⇒
        run(wrapper.mput(Nil))
        getAllKeys must beEmpty
      }
    }
    "create a new entry for each key if these keys don't exist in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (keys, values) = batch.entries.unzip
        redisClient.flushAll
        run(wrapper.mput(batch.entries))

        findEntries(keys) must containTheSameElementsAs(values)
      }
    }
    "create or update an existing entry depending on the existence of keys in cache" in {
      prop { batch: TestCacheEntryBatch ⇒
        val (existing, nonExisting) = batch.entries.splitAt(batch.entries.size / 2)
        val (existingKeys, existingValues) = existing.unzip
        val (nonExistingKeys, nonExistingValues) = nonExisting.unzip

        WithCachedData(existing) {

          val newExistingValues = existingValues map (v ⇒ TestCacheVal(v.value.reverse))

          run(wrapper.mput(nonExisting ++ existingKeys.zip(newExistingValues)))

          val insertedValues = findEntries(nonExistingKeys)
          val updatedValues = findEntries(existingKeys)

          insertedValues must containTheSameElementsAs(nonExistingValues)
          updatedValues must containTheSameElementsAs(newExistingValues)
          updatedValues must not(containTheSameElementsAs(existingValues))
        }
      }
    }
  }

}
