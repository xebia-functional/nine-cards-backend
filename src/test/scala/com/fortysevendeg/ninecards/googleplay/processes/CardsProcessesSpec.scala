package com.fortysevendeg.ninecards.googleplay.processes

import cats.data.Xor
import cats.{~>, Id}
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.domain.apigoogle._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.{apigoogle => ApiAlg, cache => CacheAlg, webscrapper => WebAlg}
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.{googleapi => ApiInt, cache => CacheInt, webscrapper => WebInt}
import com.fortysevendeg.ninecards.googleplay.service.free.{JoinServices, JoinInterpreter}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.{reset}
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CardsProcessesSpec
    extends Specification
    with Matchers
    with ScalaCheck
    with Mockito {

  import com.fortysevendeg.ninecards.googleplay.util.ScalaCheck._

  val apiGoogleIntServer: ApiInt.InterpreterServer[Id] = mock[ApiInt.InterpreterServer[Id]]
  val apiGoogleInt: ApiAlg.Ops ~> Id = ApiInt.MockInterpreter(apiGoogleIntServer)

  val cacheIntServer: CacheInt.InterpreterServer[Id] = mock[CacheInt.InterpreterServer[Id]]
  val cacheInt: CacheAlg.Ops ~> Id = CacheInt.MockInterpreter(cacheIntServer)

  val webScrapperIntServer: WebInt.InterpreterServer[Id] = mock[WebInt.InterpreterServer[Id]]
  val webScrapperInt: WebAlg.Ops ~> Id = WebInt.MockInterpreter(webScrapperIntServer)

  val interpreter: JoinServices ~> Id =
    JoinInterpreter.interpreter(apiGoogleInt, cacheInt, webScrapperInt)

  val processes = CardsProcess.processes[JoinServices]

  def clear(): Unit = {
    reset(apiGoogleIntServer)
    reset(cacheIntServer)
    reset(webScrapperIntServer)
  }

  val testDate: DateTime = new DateTime( 2015, 11, 23, 12, 0, 14, DateTimeZone.UTC)

  sequential

  "getCard" >> {

    def runGetCard( pack: Package, auth: GoogleAuthParams): getcard.Response =
      processes.getCard(pack, auth, testDate).foldMap(interpreter)

    "if the Package is already resolved" >> {

      def setup(pack: Package, card: FullCard, auth: GoogleAuthParams) = {
        clear()
        cacheIntServer.getValid( pack) returns Some(card)
      }

      "give back the card in the cache" >>
        prop { (card: FullCard, auth: GoogleAuthParams) =>
          val pack = Package(card.packageName)
          setup(pack, card, auth)
          runGetCard(pack, auth) must_== Xor.Right(card)
        }
    }

    "The package is not resolved in cache, but is available in Google API" >> {

      def setup( pack: Package, card: FullCard, auth: GoogleAuthParams) = {
        clear()
        cacheIntServer.getValid(pack) returns None
        apiGoogleIntServer.getDetails(pack, auth) returns Xor.Right(card)
        cacheIntServer.putResolved(card) returns Unit
      }

      "return the card given by the Google API and store it in the cache" >> {
        prop { (card: FullCard, auth: GoogleAuthParams) =>
          val pack = Package(card.packageName)
          setup(pack, card, auth)
          runGetCard(pack, auth) must_== Xor.Right(card)
        }
      }

      "store the card given by the Google API in the cache" >> {
        prop { (card: FullCard, auth: GoogleAuthParams) =>
          val pack = Package(card.packageName)
          setup(pack, card, auth)
          runGetCard(pack, auth)
          there was one(cacheIntServer).putResolved(card)
        }
      }
    }

    "Neither the cache nor the Google API give a card" >> {

      "If the package appears in the Web Page" >> {

        def setup( pack: Package, card: FullCard, auth: GoogleAuthParams) = {
          clear() 
          cacheIntServer.getValid(pack) returns None
          apiGoogleIntServer.getDetails(pack, auth) returns Xor.Left( PackageNotFound(pack) )
          webScrapperIntServer.existsApp(pack) returns true
          cacheIntServer.markPending(pack) returns Unit
        }

        "Return the package as PendingResolution" >>
          prop { (card: FullCard, auth: GoogleAuthParams) => 
            val pack = Package(card.packageName)
            setup(pack, card, auth)
            runGetCard(pack, auth) must_== Xor.Left( getcard.PendingResolution(pack) )
          }

        "Stores the package as Pending in the cache " >> 
          prop{  (card: FullCard, auth: GoogleAuthParams) => 
            val pack = Package(card.packageName)
            setup(pack, card, auth)
            runGetCard(pack, auth)
            there was one(cacheIntServer).markPending(pack)
          }
      }

    }

    "The package is not in the cache, the API, nor the WebPage" >> {

        def setup( pack: Package, card: FullCard, auth: GoogleAuthParams) = {
          clear() 
          cacheIntServer.getValid(pack) returns None
          apiGoogleIntServer.getDetails(pack, auth) returns Xor.Left( PackageNotFound(pack) )
          webScrapperIntServer.existsApp(pack) returns false
          cacheIntServer.markError(pack, testDate) returns Unit
        }

      "Return the package as Unresolved" >>
        prop{  (card: FullCard, auth: GoogleAuthParams) =>
          val pack = Package(card.packageName)
          setup(pack, card, auth)
          runGetCard(pack, auth) must_== Xor.Left( getcard.UnknownPackage(pack) )
        }

      "Store it in the cache as Error" >>
        prop{  (card: FullCard, auth: GoogleAuthParams) =>
          val pack = Package(card.packageName)
          setup(pack, card, auth)
          runGetCard(pack, auth)
          there was one(cacheIntServer).markError(pack, testDate)
        }

    }
  }

}


