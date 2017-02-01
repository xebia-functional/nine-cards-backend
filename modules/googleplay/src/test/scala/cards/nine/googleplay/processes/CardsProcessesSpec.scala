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
package cards.nine.googleplay.processes

import cards.nine.domain.ScalaCheck._
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.domain.market.MarketCredentials
import cards.nine.googleplay.domain.{ apigoogle ⇒ ApiDom, webscrapper ⇒ WebDom }
import cards.nine.googleplay.processes.GooglePlayApp.{ GooglePlayApp, Interpreters }
import cards.nine.googleplay.service.free.algebra.{ Cache ⇒ CacheAlg, GoogleApi ⇒ ApiAlg, WebScraper ⇒ WebAlg }
import cards.nine.googleplay.service.free.interpreter.{ cache ⇒ CacheInt, googleapi ⇒ ApiInt, webscrapper ⇒ WebInt }
import cards.nine.googleplay.util.ScalaCheck._
import cats.{ Id, ~> }
import org.joda.time.{ DateTime, DateTimeZone }
import org.mockito.Mockito.reset
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class CardsProcessesSpec
  extends Specification
  with Matchers
  with ScalaCheck
  with Mockito {

  val apiGoogleIntServer: ApiInt.InterpreterServer[Id] = mock[ApiInt.InterpreterServer[Id]]
  val apiGoogleInt: ApiAlg.Ops ~> Id = ApiInt.MockInterpreter(apiGoogleIntServer)

  val cacheIntServer: CacheInt.InterpreterServer[Id] = mock[CacheInt.InterpreterServer[Id]]
  val cacheInt: CacheAlg.Ops ~> Id = CacheInt.MockInterpreter(cacheIntServer)

  val webScrapperIntServer: WebInt.InterpreterServer[Id] = mock[WebInt.InterpreterServer[Id]]
  val webScrapperInt: WebAlg.Ops ~> Id = WebInt.MockInterpreter(webScrapperIntServer)

  val interpreter: GooglePlayApp ~> Id = Interpreters(apiGoogleInt, cacheInt, webScrapperInt)

  val processes = CardsProcesses.processes[GooglePlayApp]

  def clear(): Unit = {
    reset(apiGoogleIntServer)
    reset(cacheIntServer)
    reset(webScrapperIntServer)
  }

  val testDate: DateTime = new DateTime(2015, 11, 23, 12, 0, 14, DateTimeZone.UTC)

  sequential

  "getCard" >> {

    def runGetCard(pack: Package, auth: MarketCredentials): getcard.Response =
      processes.getCard(pack, auth).foldMap(interpreter)

    "if the Package is already resolved" >> {

      def setup(pack: Package, card: FullCard, auth: MarketCredentials) = {
        clear()
        cacheIntServer.getValid(pack) returns Some(card)
      }

      "give back the card in the cache" >>
        prop { (card: FullCard, auth: MarketCredentials) ⇒
          val pack = card.packageName
          setup(pack, card, auth)
          runGetCard(pack, auth) must beRight(card)
        }
    }

    "The package is not resolved in cache, but is available in Google API" >> {

      def setup(pack: Package, card: FullCard, auth: MarketCredentials) = {
        clear()
        cacheIntServer.getValid(pack) returns None
        apiGoogleIntServer.getDetails(pack, auth) returns Right(card)
        cacheIntServer.putResolved(card) returns Unit
      }

      "return the card given by the Google API and store it in the cache" >> {
        prop { (card: FullCard, auth: MarketCredentials) ⇒
          val pack = card.packageName
          setup(pack, card, auth)
          runGetCard(pack, auth) must beRight(card)
        }
      }

      "store the card given by the Google API in the cache" >> {
        prop { (card: FullCard, auth: MarketCredentials) ⇒
          val pack = card.packageName
          setup(pack, card, auth)
          runGetCard(pack, auth)
          there was one(cacheIntServer).putResolved(card)
        }
      }
    }

    "Neither the cache nor the Google API give a card" >> {

      "If the package appears in the Web Page" >> {

        def setup(pack: Package, card: FullCard, auth: MarketCredentials) = {
          clear()
          cacheIntServer.getValid(pack) returns None
          apiGoogleIntServer.getDetails(pack, auth) returns Left(ApiDom.PackageNotFound(pack))
          webScrapperIntServer.existsApp(pack) returns true
          cacheIntServer.setToPending(pack) returns Unit
        }

        "Return the package as PendingResolution" >>
          prop { (card: FullCard, auth: MarketCredentials) ⇒
            val pack = card.packageName
            setup(pack, card, auth)
            runGetCard(pack, auth) must beLeft(getcard.PendingResolution(pack))
          }

        "Stores the package as Pending in the cache " >>
          prop { (card: FullCard, auth: MarketCredentials) ⇒
            val pack = card.packageName
            setup(pack, card, auth)
            runGetCard(pack, auth)
            there was one(cacheIntServer).setToPending(pack)
          }
      }

    }

    "The package is not in the cache, the API, nor the WebPage" >> {

      def setup(pack: Package, card: FullCard, auth: MarketCredentials) = {
        clear()
        cacheIntServer.getValid(pack) returns None
        apiGoogleIntServer.getDetails(pack, auth) returns Left(ApiDom.PackageNotFound(pack))
        webScrapperIntServer.existsApp(pack) returns false
        cacheIntServer.addError(pack) returns Unit
      }

      "Return the package as Unresolved" >>
        prop { (card: FullCard, auth: MarketCredentials) ⇒
          val pack = card.packageName
          setup(pack, card, auth)
          runGetCard(pack, auth) must beLeft(getcard.UnknownPackage(pack))
        }

      "Store it in the cache as Error" >>
        prop { (card: FullCard, auth: MarketCredentials) ⇒
          val pack = card.packageName
          setup(pack, card, auth)
          runGetCard(pack, auth)
          there was one(cacheIntServer).addError(pack)
        }

    }
  }

  "resolvePendingPackage" should {

    def runResolvePending(pack: Package): ResolvePending.PackageStatus =
      processes.resolvePendingPackage(pack).foldMap(interpreter)

    "when the WebScrapper gives back a full card" >> {

      def setup(pack: Package, card: FullCard) = {
        clear()
        webScrapperIntServer.getDetails(pack) returns Right(card)
        cacheIntServer.putResolved(card) returns Unit
      }

      "report the package as Resolved and store it in the cache" >>
        prop { card: FullCard ⇒
          val pack = card.packageName
          setup(pack, card)
          runResolvePending(pack) must_=== ResolvePending.Resolved
        }

      "store the result of the web scrape in the cache" >>
        prop { card: FullCard ⇒
          val pack = card.packageName
          setup(pack, card)
          runResolvePending(pack)
          there was one(cacheIntServer).putResolved(card)
        }

    }

    "when the WebScrapper can no longer find the package" >> {

      def setup(pack: Package, date: DateTime) = {
        clear()
        webScrapperIntServer.getDetails(pack) returns Left(WebDom.PackageNotFound(pack))
        cacheIntServer.addError(pack) returns Unit
      }

      "it reports the package as Unknown" >> prop { pack: Package ⇒
        setup(pack, testDate)
        runResolvePending(pack) must_=== ResolvePending.Unknown
      }

      "it stores the package as an error" >> prop { pack: Package ⇒
        setup(pack, testDate)
        runResolvePending(pack)
        there was one(cacheIntServer).addError(pack)
      }

    }

    "when the WebScrapper has another kind of error" >> {

      def setup(pack: Package) = {
        clear()
        webScrapperIntServer.getDetails(pack) returns Left(WebDom.WebPageServerError)
        cacheIntServer.setToPending(pack) returns Unit
      }

      "it reports the package as (still) pending" >> prop { pack: Package ⇒
        setup(pack)
        runResolvePending(pack) must_=== ResolvePending.Pending
      }

    }

  }

}
