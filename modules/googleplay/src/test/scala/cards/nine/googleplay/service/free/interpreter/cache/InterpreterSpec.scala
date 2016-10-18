package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.Package
import cards.nine.googleplay.domain.FullCard
import cards.nine.googleplay.service.free.algebra.Cache._
import cards.nine.googleplay.util.{ ScalaCheck ⇒ CustomArbitrary }
import com.redis.RedisClient
import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.specification.{ AfterAll, BeforeAll, BeforeEach }
import redis.embedded.RedisServer

import scala.concurrent.duration._

class InterpreterSpec
  extends Specification
  with ScalaCheck
  with BeforeAll
  with BeforeEach
  with AfterAll {

  import CirceCoders._
  import CustomArbitrary._
  import KeyType._
  import io.circe.syntax._
  import cards.nine.domain.application.ScalaCheck._

  private[this] object setup {
    val redisServer: RedisServer = new RedisServer()
    redisServer.start()
    val redisClient: RedisClient = new RedisClient(host = "localhost", port = redisServer.getPort())

    def flush = redisClient.flushall

    val interpreter = CacheInterpreter

    def eval[A](op: Ops[A]) = interpreter(op)(redisClient).unsafePerformSync

    def evalWithDelay[A](op: Ops[A], delay: Duration) = interpreter(op)(redisClient).after(delay).unsafePerformSync

    def keyPattern(p: String, t: String, d: String) =
      s"""{"package":"$p","keyType":"$t","date":$d}""".trim

    def writeKey(t: KeyType, p: String, d: String) = keyPattern(p, t.entryName, d)

    def resolvedKey(p: Package): String = keyPattern(p.value, Resolved.entryName, "null")
    def pendingKey(p: String): String = keyPattern(p, Pending.entryName, "null")
    def errorKey(p: String, d: String) = keyPattern(p, Error.entryName, d)

    def allByType(keyType: KeyType) = keyPattern("*", keyType.entryName, "*")
    def allByPackage(p: Package): String = keyPattern(p.value, "*", "*")
    def allByPackageAndType(p: Package, keyType: KeyType): String = keyPattern(p.value, keyType.entryName, "*")

    val date: DateTime = new DateTime(2016, 7, 23, 12, 0, 14, DateTimeZone.UTC)
    val dateJsonStr = s""" "16072312001400" """.trim

  }

  import setup._

  override def beforeAll = {}

  override def afterAll = redisServer.stop()

  override def before = flush

  sequential

  private def putEntry(e: CacheEntry) = redisClient.set(e._1.asJson.noSpaces, e._2.asJson.noSpaces)

  "getValidCard" should {

    "return None if the Cache is empty" >>
      prop { pack: Package ⇒
        eval(GetValid(pack)) must beNone
      }

    "return None if the cache only contains Error or Pending entries for the package" >>
      prop { pack: Package ⇒
        putEntry(CacheEntry.pending(pack))
        putEntry(CacheEntry.error(pack, date))
        eval(GetValid(pack)) must beNone
      }

    "return Some(e) if the cache contains a Resolved entry" >>
      prop { card: FullCard ⇒
        putEntry(CacheEntry.resolved(card))
        eval(GetValid(card.packageName)) must beSome(card)
      }

    "return Some(e) if the cache contains a Permanent entry" >>
      prop { card: FullCard ⇒
        putEntry(CacheEntry.permanent(card.packageName, card))
        eval(GetValid(card.packageName)) must beSome(card)
      }

  }

  "putResolved" should {

    "add a package as resolved" >>
      prop { card: FullCard ⇒
        flush
        redisClient.get(resolvedKey(card.packageName)) must beNone
        eval(PutResolved(card))
        redisClient.get(resolvedKey(card.packageName)) must beSome
      }

    "add no other key as resolved" >>
      prop { (card: FullCard, pack: Package) ⇒
        flush
        eval(PutResolved(card))
        val actual = redisClient.get(resolvedKey(pack))
        if (card.packageName == pack.value) actual must beSome else actual must beNone
      }

    "add no key as pending or error" >>
      prop { card: FullCard ⇒
        flush
        eval(PutResolved(card))
        redisClient.keys(allByType(Pending)) must_=== Some(List())
        redisClient.keys(allByType(Error)) must_=== Some(List())
      }

    "overwrite any previous value" >>
      prop { (card_1: FullCard, card_2x: FullCard) ⇒
        flush
        val card_2 = card_2x.copy(packageName = card_1.packageName)
        eval(PutResolved(card_1))
        eval(PutResolved(card_2))
        val actual = redisClient.get(resolvedKey(card_1.packageName))
        redisClient.keys(allByType(Resolved)) must beSome.which(_.length === 1)
        actual must_=== Some(cacheValE(CacheVal(Some(card_2))).noSpaces)
      }
  }

  "markPending" should {
    "add a package as Pending" >>
      prop { pack: Package ⇒
        flush
        eval(MarkPending(pack))
        redisClient.get(pendingKey(pack.value)) must beSome
      }

    "add no key as resolved or error" >>
      prop { pack: Package ⇒
        flush
        eval(MarkPending(pack))
        redisClient.keys(allByType(Error)) must_=== Some(List())
        redisClient.keys(allByType(Resolved)) must_=== Some(List())
      }
  }

  "unmarkPending" should {
    "remove a previously pending package" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.pending(pack))
        eval(MarkPending(pack))
        redisClient.get(CacheKey.pending(pack)) must beNone
      }
  }

  "markError" should {
    "add a package as error" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))
        redisClient.keys(allByPackageAndType(pack, Error)) must beSome
      }

    "add no key as resolved or pending" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))
        redisClient.keys(allByType(Resolved)) must_=== Some(List())
        redisClient.keys(allByType(Pending)) must_=== Some(List())
      }

    "allow adding several error entries for package with different dates" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))
        evalWithDelay(MarkError(pack), 1.millis)
        redisClient.keys(allByType(Error)) must beSome.which(_.length === 2)
      }

  }

  "ClearInvalid" should {

    "remove existing error entries" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.error(pack, date))
        eval(ClearInvalid(pack))
        redisClient.keys(allByPackage(pack)) must beSome.which(_.isEmpty)
      }

    "remove existing pending entries" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.pending(pack))
        eval(ClearInvalid(pack))
        redisClient.keys(allByPackage(pack)) must beSome.which(_.isEmpty)
      }

    "leave resolved entries" >>
      prop { card: FullCard ⇒
        val pack = card.packageName
        flush
        putEntry(CacheEntry.resolved(card))
        eval(ClearInvalid(pack))
        redisClient.get(resolvedKey(card.packageName)) must beSome
      }

    "leave error entries for any other package" >>
      prop { pack: Package ⇒
        val other = Package(s"${pack.value}_not")
        flush
        putEntry(CacheEntry.error(pack, date))
        eval(ClearInvalid(other))
        redisClient.keys(allByPackage(pack)) must beSome.which(_.nonEmpty)
      }
  }

  "List Pending" should {
    import org.scalacheck.{ Arbitrary, Gen }

    case class ListPendingTest(allPending: List[Package], num: Int)

    val listPendingGen: Gen[ListPendingTest] = for /*Gen*/ {
      allPending ← Gen.listOf(arbPackage.arbitrary)
      num ← Gen.Choose.chooseInt.choose(0, allPending.length)
    } yield ListPendingTest(allPending, num)

    implicit val listPendingArb: Arbitrary[ListPendingTest] = Arbitrary(listPendingGen)

    "get all the pending elements for a package" >>
      prop { testData: ListPendingTest ⇒
        import testData._
        flush
        allPending.map(CacheEntry.pending).foreach(putEntry)
        val list = eval(ListPending(num))
        list must contain((p: Package) ⇒ allPending must contain(p)).forall
        list must have size Math.min(num, allPending.size)
      }
  }

}
