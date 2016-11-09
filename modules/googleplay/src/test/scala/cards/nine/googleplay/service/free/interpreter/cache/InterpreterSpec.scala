package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.ScalaCheck._
import cards.nine.googleplay.service.free.algebra.Cache._
import cards.nine.googleplay.util.{ ScalaCheck ⇒ CustomArbitrary }
import com.redis.RedisClient
import io.circe.parser._
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalacheck.{ Arbitrary, Gen }
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

  private[this] object setup {
    lazy val redisServer: RedisServer = new RedisServer()
    lazy val redisClient: RedisClient = new RedisClient(host = "localhost", port = redisServer.getPort)

    def flush = redisClient.flushall

    val interpreter = CacheInterpreter

    def eval[A](op: Ops[A]) = interpreter(op)(redisClient).unsafePerformSync

    def evalWithDelay[A](op: Ops[A], delay: Duration) = interpreter(op)(redisClient).after(delay).unsafePerformSync

    def pendingKey(pack: Package): String = s"${pack.value}:Pending"
    def resolvedKey(pack: Package): String = s"${pack.value}:Resolved"

    def allByType(keyType: KeyType) = s"*:${keyType.entryName}*"
    def allByPackage(pack: Package): String = s"${pack.value}:*"
    def allByPackageAndType(pack: Package, keyType: KeyType): String = s"${pack.value}:${keyType.entryName}*"

    val date: DateTime = new DateTime(2016, 7, 23, 12, 0, 14, DateTimeZone.UTC)
    val dateJsonStr = s""" "16072312001400" """.trim

  }

  import setup._

  override def beforeAll = redisServer.start()

  override def afterAll = redisServer.stop()

  override def before = flush

  sequential

  private def getCacheValue(key: String) =
    redisClient.get(key).flatMap(e ⇒ decode[CacheVal](e).toOption)

  private def getCacheValues(keys: List[String]) = keys match {
    case head :: tail ⇒
      redisClient.mget(head, tail: _*)
        .getOrElse(Nil)
        .flatMap(_.toList)
        .map(e ⇒ decode[CacheVal](e).toOption)
    case _ ⇒ Nil
  }
  private def getEntry(key: String) = redisClient.get(key)
  private def getEntries(keys: List[String]) = keys match {
    case head :: tail ⇒ redisClient.mget(head, tail: _*).getOrElse(Nil).flatMap(_.toList)
    case _ ⇒ Nil
  }
  private def getKeys(pattern: String) = redisClient.keys(pattern).getOrElse(Nil)
  private def putEntry(e: CacheEntry) =
    redisClient.set(KeyFormat.format(e._1), cacheValE(e._2).noSpaces)
  private def putEntries(entries: List[CacheEntry]) = entries match {
    case Nil ⇒ Unit
    case _ ⇒ redisClient.mset(entries map formatEntry: _*)
  }
  private def formatEntry(e: CacheEntry): (String, String) =
    (KeyFormat.format(e._1), cacheValE(e._2).noSpaces)

  "getValidCard" should {

    "return None if the Cache is empty" >>
      prop { pack: Package ⇒
        flush

        eval(GetValid(pack)) must beNone
      }

    "return None if the cache only contains Error or Pending entries for the package" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.pending(pack))
        putEntry(CacheEntry.error(pack, date))

        eval(GetValid(pack)) must beNone
      }

    "return Some(e) if the cache contains a Resolved entry" >>
      prop { card: FullCard ⇒
        flush
        putEntry(CacheEntry.resolved(card))

        eval(GetValid(card.packageName)) must beSome(card)
      }

    "return Some(e) if the cache contains a Permanent entry" >>
      prop { card: FullCard ⇒
        flush
        putEntry(CacheEntry.permanent(card.packageName, card))

        eval(GetValid(card.packageName)) must beSome(card)
      }
  }

  "getValidMany" should {

    "return an empty list if the cache is empty" >>
      prop { packages: List[Package] ⇒
        flush

        eval(GetValidMany(packages)) must beEmpty
      }

    "return an empty list if the cache only contains Error or Pending entries for the packages" >>
      prop { packages: List[Package] ⇒
        flush
        putEntries(packages map CacheEntry.pending)
        putEntries(packages map (p ⇒ CacheEntry.error(p, date)))

        eval(GetValidMany(packages)) must beEmpty
      }

    "return a list of cards if the cache contains a Resolved entry" >>
      prop { cards: List[FullCard] ⇒
        flush
        putEntries(cards map CacheEntry.resolved)

        eval(GetValidMany(cards map (_.packageName))) must containTheSameElementsAs(cards)
      }

    "return a list of cards if the cache contains a Permanent entry" >>
      prop { cards: List[FullCard] ⇒
        flush
        putEntries(cards map (c ⇒ CacheEntry.permanent(c.packageName, c)))

        eval(GetValidMany(cards map (_.packageName))) must containTheSameElementsAs(cards)
      }
  }

  "putResolved" should {

    "add a package as resolved" >>
      prop { card: FullCard ⇒
        flush
        eval(PutResolved(card))

        getEntry(resolvedKey(card.packageName)) must beSome
      }

    "add no other key as resolved" >>
      prop { (card: FullCard, pack: Package) ⇒
        flush
        eval(PutResolved(card))

        getEntry(resolvedKey(pack)) must beNone
      }

    "add no key as pending or error" >>
      prop { card: FullCard ⇒
        flush
        eval(PutResolved(card))

        getKeys(allByType(Pending)) must beEmpty
        getKeys(allByType(Error)) must beEmpty
      }

    "overwrite any previous value" >>
      prop { (card: FullCard) ⇒
        flush
        val newCard = card.copy(title = card.title.reverse, free = !card.free)
        eval(PutResolved(card))
        eval(PutResolved(newCard))

        getKeys(allByType(Resolved)) must haveSize(1)
        getCacheValue(resolvedKey(card.packageName)) must_=== Option(CacheVal(Option(newCard)))
      }
  }

  "putResolvedMany" should {

    "add a list of packages as resolved" >>
      prop { cards: List[FullCard] ⇒
        flush
        eval(PutResolvedMany(cards))

        getEntries(cards map (c ⇒ resolvedKey(c.packageName))) must haveSize(cards.size)
      }

    "add no other key as resolved" >>
      prop { (cards: List[FullCard], pack: Package) ⇒
        flush
        eval(PutResolvedMany(cards))

        getEntry(resolvedKey(pack)) must beNone
      }

    "add no key as pending or error" >>
      prop { cards: List[FullCard] ⇒
        flush
        eval(PutResolvedMany(cards))

        getKeys(allByType(Pending)) must beEmpty
        getKeys(allByType(Error)) must beEmpty
      }

    "overwrite any previous value" >>
      prop { (cards: List[FullCard]) ⇒
        flush
        val newCards = cards map (c ⇒ c.copy(title = c.title.reverse, free = !c.free))
        eval(PutResolvedMany(cards))
        eval(PutResolvedMany(newCards))

        val values = getCacheValues(cards map (c ⇒ resolvedKey(c.packageName)))

        getKeys(allByType(Resolved)) must haveSize(cards.size)
        values must containTheSameElementsAs(newCards map (c ⇒ Option(CacheVal(Option(c)))))
      }
  }

  "markPending" should {
    "add a package as Pending" >>
      prop { pack: Package ⇒
        flush
        eval(MarkPending(pack))

        getEntry(pendingKey(pack)) must beSome
      }

    "add no key as resolved or error" >>
      prop { pack: Package ⇒
        flush
        eval(MarkPending(pack))

        getKeys(allByType(Error)) must beEmpty
        getKeys(allByType(Resolved)) must beEmpty
      }
  }

  "markPendingMany" should {
    "add a list of packages as Pending" >>
      prop { packages: List[Package] ⇒
        flush
        eval(MarkPendingMany(packages))

        getEntries(packages map pendingKey) must haveSize(packages.size)
      }

    "add no key as resolved or error" >>
      prop { packages: List[Package] ⇒
        flush
        eval(MarkPendingMany(packages))

        getKeys(allByType(Error)) must beEmpty
        getKeys(allByType(Resolved)) must beEmpty
      }
  }

  "unmarkPending" should {
    "remove a previously pending package" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.pending(pack))
        eval(UnmarkPending(pack))

        getEntry(pendingKey(pack)) must beNone
      }
  }

  "unmarkPendingMany" should {
    "remove a previously list of pending packages" >>
      prop { packages: List[Package] ⇒
        flush
        putEntries(packages map CacheEntry.pending)
        eval(UnmarkPendingMany(packages))

        getEntries(packages map pendingKey) must beEmpty
      }
  }

  "markError" should {
    "add a package as error" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))

        getKeys(allByPackageAndType(pack, Error)) must not be empty
      }

    "add no key as resolved or pending" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))

        getKeys(allByType(Resolved)) must beEmpty
        getKeys(allByType(Pending)) must beEmpty
      }

    "allow adding several error entries for package with different dates" >>
      prop { pack: Package ⇒
        flush
        eval(MarkError(pack))
        evalWithDelay(MarkError(pack), 1.millis)

        getKeys(allByType(Error)) must haveSize(2)
      }
  }

  "markErrorMany" should {
    "add a list of packages as error" >>
      prop { packages: List[Package] ⇒
        flush
        eval(MarkErrorMany(packages))

        getKeys(allByType(Error)) must haveSize(packages.size)
      }

    "add no key as resolved or pending" >>
      prop { packages: List[Package] ⇒
        flush
        eval(MarkErrorMany(packages))

        getKeys(allByType(Pending)) must beEmpty
        getKeys(allByType(Resolved)) must beEmpty
      }

    "allow adding several error entries for package with different dates" >>
      prop { packages: List[Package] ⇒
        flush
        eval(MarkErrorMany(packages))
        evalWithDelay(MarkErrorMany(packages), 2.millis)

        getKeys(allByType(Error)) must haveSize(packages.size * 2)
      }
  }

  "ClearInvalid" should {

    "remove existing error entries" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.error(pack, date))
        eval(ClearInvalid(pack))

        getKeys(allByPackage(pack)) must beEmpty
      }

    "remove existing pending entries" >>
      prop { pack: Package ⇒
        flush
        putEntry(CacheEntry.pending(pack))
        eval(ClearInvalid(pack))

        getKeys(allByPackage(pack)) must beEmpty
      }

    "leave resolved entries" >>
      prop { card: FullCard ⇒
        flush
        putEntry(CacheEntry.resolved(card))
        eval(ClearInvalid(card.packageName))

        getEntry(resolvedKey(card.packageName)) must beSome
      }

    "leave error entries for any other package" >>
      prop { pack: Package ⇒
        val other = Package(s"${pack.value}_not")
        flush
        putEntry(CacheEntry.error(pack, date))
        eval(ClearInvalid(other))

        getKeys(allByPackage(pack)) must not be empty
      }
  }

  "ClearInvalidMany" should {

    "remove existing error entries" >>
      prop { packages: List[Package] ⇒
        flush
        putEntries(packages map (p ⇒ CacheEntry.error(p, date)))
        eval(ClearInvalidMany(packages))

        getKeys(allByType(Error)) must beEmpty
      }

    "remove existing pending entries" >>
      prop { packages: List[Package] ⇒
        flush
        putEntries(packages map CacheEntry.pending)
        eval(ClearInvalidMany(packages))

        getKeys(allByType(Pending)) must beEmpty
      }

    "leave resolved entries" >>
      prop { cards: List[FullCard] ⇒
        flush
        putEntries(cards map CacheEntry.resolved)
        eval(ClearInvalidMany(cards map (_.packageName)))

        getKeys(allByType(Resolved)) must haveSize(cards.size)
      }

    "leave error entries for any other package" >>
      prop { packages: List[Package] ⇒
        flush
        val otherPackages = List(Package("other.package"))
        putEntries((packages ++ otherPackages) map (p ⇒ CacheEntry.error(p, date)))
        eval(ClearInvalidMany(otherPackages))

        getKeys(allByType(Error)) must haveSize(packages.size)
      }
  }

  "List Pending" should {

    case class ListPendingTestData(pendingPackages: List[Package], limit: Int)

    implicit val listPendingArb: Arbitrary[ListPendingTestData] = Arbitrary {
      for {
        pendingPackages ← Gen.listOf(arbPackage.arbitrary)
        limit ← Gen.Choose.chooseInt.choose(0, pendingPackages.length)
      } yield ListPendingTestData(pendingPackages, limit)
    }

    "get all the pending elements for a package" >>
      prop { testData: ListPendingTestData ⇒
        flush
        putEntries(testData.pendingPackages map CacheEntry.pending)
        val list = eval(ListPending(testData.limit))

        list must contain(atMost(testData.pendingPackages: _*))
        list must haveSize(testData.limit)
      }
  }

}
