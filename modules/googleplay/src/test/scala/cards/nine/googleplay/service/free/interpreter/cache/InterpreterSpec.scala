package cards.nine.googleplay.service.free.interpreter.cache

import cards.nine.commons.redis.TestUtils
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.ScalaCheck._
import cards.nine.googleplay.service.free.algebra.Cache._
import cards.nine.googleplay.util.{ ScalaCheck ⇒ CustomArbitrary }
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalacheck.{ Arbitrary, Gen }
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.specification.{ AfterAll, BeforeAll, BeforeEach }
import redis.embedded.RedisServer
import scala.concurrent.{ Await, Future } // FIXME
import scala.concurrent.duration._
import scredis.{ Client ⇒ ScredisClient, TransactionBuilder }

class InterpreterSpec
  extends Specification
  with ScalaCheck
  with BeforeAll
  with BeforeEach
  with AfterAll {

  import Formats._
  import CustomArbitrary._
  import KeyType._

  import TestUtils.redisTestActorSystem

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] object setup {
    lazy val redisServer: RedisServer = new RedisServer()
    lazy val redisClient: ScredisClient = ScredisClient(host = "localhost", port = redisServer.getPort)

    def flush = redisClient.flushAll

    import scala.concurrent.ExecutionContext.Implicits.global

    val interpreter = new CacheInterpreter()

    def eval[A](op: Ops[A]) = interpreter(op)(redisClient).unsafePerformSync

    def evalWithDelay[A](op: Ops[A], delay: Duration) = interpreter(op)(redisClient).after(delay).unsafePerformSync

    def pendingKey(pack: Package): String = s"${pack.value}:Pending"
    def resolvedKey(pack: Package): String = s"${pack.value}:Resolved"

    def permanentKey(pack: Package): String = s"${pack.value}:Permanent"
    def allByType(keyType: KeyType) = s"*:${keyType.entryName}*"
    val allErrors = "*:Error"
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

  def await[A](fut: Future[A]) = Await.result(fut, Duration.Inf)

  private def getCacheValue(key: String): Option[CacheVal] =
    await(redisClient.get[Option[CacheVal]](key).map(_.flatten))

  private def getCacheValues(keys: List[String]): List[Option[CacheVal]] =
    if (keys.isEmpty)
      Nil
    else
      await(redisClient.mGet[Option[CacheVal]](keys: _*).map(_.map(_.flatten)))

  private def existsEntry(key: String): Boolean =
    await(redisClient.get[String](key).map(_.isDefined))

  private def getKeys(pattern: String): List[CacheKey] =
    await(redisClient.keys(pattern)).toList.flatMap(parseKey)

  private def putEntry(e: (CacheKey, CacheVal)): Unit =
    await(redisClient.set(formatKey(e._1), cacheValE(e._2).noSpaces))

  private def putError(p: Package, d: DateTime): Unit = {
    await(redisClient.rPush[DateTime](formatKey(CacheKey.error(p)), d))
    Unit
  }

  private def getPending(): List[Package] =
    await(redisClient.sMembers[Option[Package]]("pending_packages").map(_.toList.flatten))

  private def putPendings(ps: List[Package]): Unit =
    if (!ps.isEmpty) {
      await(redisClient.sAdd("pending_packages", ps.map(_.value): _*))
    }

  private def putPending(p: Package): Unit =
    await(redisClient.sAdd("pending_packages", p))

  private def dumpQueue(): List[Package] =
    await(redisClient.lRange[Option[Package]]("pending_packages", 0, -1)).flatten

  private def putErrors(ps: List[Package], d: DateTime): Unit = await {
    redisClient.inTransaction { tb: TransactionBuilder ⇒
      ps.foreach(p ⇒ tb.rPush[DateTime](formatKey(CacheKey.error(p)), d))
    }.map(_ ⇒ Unit)
  }

  private def putEntries(entries: List[(CacheKey, CacheVal)]): Unit = {
    def formatEntry(e: (CacheKey, CacheVal)): (String, String) =
      (formatKey(e._1), cacheValE(e._2).noSpaces)
    await { redisClient.mSet(entries.map(formatEntry).toMap) }
    Unit
  }

  "getValidCard" should {

    "return None if the Cache is empty" >>
      prop { pack: Package ⇒
        flush

        eval(GetValid(pack)) must beNone
      }

    "return None if the cache only contains Error or Pending entries for the package" >>
      prop { pack: Package ⇒
        flush
        putPending(pack)
        putError(pack, date)

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
        putEntry(CacheEntry.permanent(card))

        eval(GetValid(card.packageName)) must beSome(card)
      }
  }

  "getValidMany" should {

    "return an empty list if the cache is empty" >>
      prop { packages: List[Package] ⇒
        flush

        eval(GetValidMany(packages)) must beEmpty
      }

    "return an empty list if the cache only contains the packages as Errors" >>
      prop { packages: List[Package] ⇒
        flush
        packages foreach { p ⇒ putError(p, date) }

        eval(GetValidMany(packages)) must beEmpty
      }

    "return an empty list if the cache only contains the packages as Pending" >>
      prop { packages: List[Package] ⇒
        flush
        putPendings(packages)

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
        putEntries(cards map CacheEntry.permanent)

        eval(GetValidMany(cards map (_.packageName))) must containTheSameElementsAs(cards)
      }
  }

  "putResolved" should {

    "add a package as resolved" >>
      prop { card: FullCard ⇒
        flush
        eval(PutResolved(card))

        existsEntry(resolvedKey(card.packageName)) must beTrue
      }

    "add no other key as resolved" >>
      prop { (card: FullCard, pack: Package) ⇒
        flush
        eval(PutResolved(card))

        existsEntry(resolvedKey(pack)) must beFalse
      }

    "add no key as pending or error" >>
      prop { card: FullCard ⇒
        flush
        eval(PutResolved(card))

        getPending() must beEmpty
        getKeys(allByType(Error)) must beEmpty
      }

    "remove any previous pending or error keys for the package" >>
      prop { card: FullCard ⇒
        flush
        putPending(card.packageName)
        putError(card.packageName, date)
        eval(PutResolved(card))
        getPending() must beEmpty
        getKeys(allByType(Error)) must beEmpty
        dumpQueue() must beEmpty
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
        cards.map(c ⇒ resolvedKey(c.packageName))
          .filter(existsEntry _) must haveSize(cards.size)
      }

    "add no other key as resolved" >>
      prop { (cards: List[FullCard], pack: Package) ⇒
        flush
        eval(PutResolvedMany(cards))

        existsEntry(resolvedKey(pack)) must beFalse
      }

    "add no key as pending or error" >>
      prop { cards: List[FullCard] ⇒
        flush
        eval(PutResolvedMany(cards))

        getPending() must beEmpty
        getKeys(allByType(Error)) must beEmpty
      }

    "overwrite any previous value" >>
      prop { (cards: List[FullCard]) ⇒
        flush
        val newCards = cards map (c ⇒ c.copy(title = c.title.reverse, free = !c.free))
        eval(PutResolvedMany(cards))
        evalWithDelay(PutResolvedMany(newCards), 1.millis)

        val values = getCacheValues(cards map (c ⇒ resolvedKey(c.packageName)))

        getKeys(allByType(Resolved)) must haveSize(cards.size)
        values must containTheSameElementsAs(newCards map (c ⇒ Option(CacheVal(Option(c)))))
      }
  }

  "putPermanent" should {
    "add a package as permanent" >>
      prop { card: FullCard ⇒
        flush
        eval(PutPermanent(card))

        existsEntry(permanentKey(card.packageName)) must beTrue
      }

    "add no other key as permanent" >>
      prop { (card: FullCard, pack: Package) ⇒
        flush
        eval(PutPermanent(card))

        existsEntry(permanentKey(pack)) must beFalse
      }

    "add no key as pending or error" >>
      prop { card: FullCard ⇒
        flush
        eval(PutPermanent(card))
        getPending() must beEmpty
        getKeys(allByType(Error)) must beEmpty
      }

    "remove any previous pending or error keys for the package" >>
      prop { card: FullCard ⇒
        flush
        putPending(card.packageName)
        putError(card.packageName, date)
        eval(PutPermanent(card))
        getPending() must beEmpty
        getKeys(allByType(Error)) must beEmpty
        dumpQueue() must beEmpty
      }

    "overwrite any previous value" >>
      prop { (card: FullCard) ⇒
        flush
        val newCard = card.copy(title = card.title.reverse, free = !card.free)
        eval(PutPermanent(card))
        eval(PutPermanent(newCard))

        getKeys(allByType(Permanent)) must haveSize(1)
        getCacheValue(permanentKey(card.packageName)) must_=== Option(CacheVal(Option(newCard)))
      }
  }

  "addError" should {
    "add a package as error" >>
      prop { pack: Package ⇒
        flush
        eval(AddError(pack))

        getKeys(allByPackageAndType(pack, Error)) must not be empty
      }

    "add no key as resolved or pending" >>
      prop { pack: Package ⇒
        flush
        eval(AddError(pack))

        getKeys(allByType(Resolved)) must beEmpty
        getPending() must beEmpty
      }

    "remove any previous pending keys for the package" >>
      prop { pack: Package ⇒
        flush
        putPending(pack)
        eval(AddError(pack))
        getPending() must beEmpty
        dumpQueue() must beEmpty
      }

    "allow adding several error entries for package with different dates" >>
      prop { pack: Package ⇒
        flush
        eval(AddError(pack))
        evalWithDelay(AddError(pack), 1.millis)
        getKeys(allByType(Error)) must haveSize(1)
      }
  }

  "addErrorMany" should {
    "add a list of packages as error" >>
      prop { packages: List[Package] ⇒
        flush
        eval(AddErrorMany(packages))

        getKeys(allByType(Error)) must haveSize(packages.size)
      }

    "add no key as resolved or pending" >>
      prop { packages: List[Package] ⇒
        flush
        eval(AddErrorMany(packages))

        getPending() must beEmpty
        getKeys(allByType(Resolved)) must beEmpty
      }

    "remove any previous pending keys for the packages" >>
      prop { packages: List[Package] ⇒
        flush
        putPendings(packages)
        eval(AddErrorMany(packages))
        getPending() must beEmpty
        dumpQueue() must beEmpty
      }

    "allow adding several errors with different dates" >>
      prop { packages: List[Package] ⇒
        flush
        eval(AddErrorMany(packages))
        evalWithDelay(AddErrorMany(packages), 2.millis)
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
        putPendings(testData.pendingPackages)
        val list = eval(ListPending(testData.limit))
        list must contain(atMost(testData.pendingPackages: _*))
        list must haveSize(testData.limit)
      }
  }
}
