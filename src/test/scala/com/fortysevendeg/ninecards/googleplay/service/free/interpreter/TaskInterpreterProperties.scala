package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.{ BulkRequestPackage, RequestPackage }
import org.scalacheck._
import org.scalacheck.Prop._
import org.scalacheck.Shapeless._
import scalaz.concurrent.Task
import cats.data.Xor

object TaskInterpreterProperties extends Properties("Task interpreter") {

  import org.scalacheck.Arbitrary
  import org.scalacheck.Arbitrary._
  import org.scalacheck.Gen._

  implicit val arbItem: Arbitrary[Item] = Arbitrary(for {
    title <- identifier
    docId <- identifier
  } yield Item(
    DocV2(
      title   = title,
      creator = "",
      docid   = docId,
      details = Details(
        appDetails = AppDetails(
          appCategory  = List(),
          numDownloads = "",
          permission   = List()
        )
      ),
      aggregateRating = AggregateRating(
        ratingsCount     = 0,
        oneStarRatings   = 0,
        twoStarRatings   = 0,
        threeStarRatings = 0,
        fourStarRatings  = 0,
        fiveStarRatings  = 0,
        starRating       = 0.0
      ),
      image = List(),
      offer = List()
    )
  ))


  property("Requesting a single package should pass the correct parameters to the client request") = forAll { (pkg: Package, i: Item, t: Token, id: AndroidId, l: Localization, b: Boolean) =>

    val lo = if(b) Some(l) else None // well this is horrible

    val request = RequestPackage((t, id, lo), pkg)

    val f: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (pkgParam, (tParam, idParam, loParam)) =>
      Task.now {
        (pkgParam, tParam, idParam, loParam) match {
          case (`pkg`, `t`, `id`, `lo`) => Xor.right(i)
          case _ => Xor.left(pkgParam.value)
        }
      }
    }

    val interpreter = TaskInterpreter.interpreter(f)

    val response = interpreter(request)

    response.run ?= Some(i)
  }

  property("Requesting multiple packages should call the API for the given packages and no others") = forAllNoShrink { (ps: List[Package], i: Item, t: Token, id: AndroidId) =>

    val packageNames = ps.map(_.value)

    val request = BulkRequestPackage((t, id, None), PackageListRequest(packageNames))

    val f: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (pkgParam, (tParam, idParam, loParam)) =>
      Task.now {
        (tParam, idParam, loParam) match {
          case (`t`, `id`, None) if(ps.contains(pkgParam)) => Xor.right(i)
          case _ => Xor.left(pkgParam.value)
        }
      }
    }

    val interpreter = TaskInterpreter.interpreter(f)

    val response = interpreter(request)

    val packageDetails = response.run

    (s"Should have not errored for any request: ${packageDetails.errors}" |: (packageDetails.errors ?= Nil)) &&
    (s"Should have successfully returned for each given package: ${packageDetails.items.length}" |: (packageDetails.items.length ?= ps.length))
  }
}

/*
 * Scenarios to test
 *  Failed task on api calls web
 *  Xor left on api then right on web
 *  Xor right on api does not call web
 *  Failed task on both makes an Xor left
 * 
 * The above for both single and bulk
 */
