package com.fortysevendeg.ninecards.googleplay.service.free.interpreter

import com.fortysevendeg.ninecards.googleplay.domain.Domain._
import com.fortysevendeg.ninecards.googleplay.service.GooglePlayDomain._
import com.fortysevendeg.ninecards.googleplay.service.free.algebra.GooglePlay.RequestPackage
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


  property("Requesting a single package should pass the correct parameters to the client request") = forAll { (pkg: Package, t: Token, id: AndroidId, i: Item) =>

    val request = RequestPackage((t, id, None), pkg)

    val f: (Package, GoogleAuthParams) => Task[Xor[String, Item]] = { case (pkgParam, (tParam, idParam, loParam)) =>
      Task.now {
        (pkgParam, tParam, idParam, loParam) match {
          case (`pkg`, `t`, `id`, None) => Xor.right(i)
          case _ => Xor.left(pkgParam.value)
        }
      }
    }

    val interpreter = TaskInterpreter.interpreter(f)

    val response = interpreter(request)

    response.run ?= Some(i)
  }
}

/*
 * Scenarios to test
 *  Failed task on api calls web
 *  Xor left on api then right on web
 *  Xor right on api does not call web
 *  Failed task on both makes an Xor left
 */
