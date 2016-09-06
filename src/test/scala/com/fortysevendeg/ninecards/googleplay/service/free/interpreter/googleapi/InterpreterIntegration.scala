package com.fortysevendeg.ninecards.googleplay.service.free.interpreter.googleapi

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain._
import com.fortysevendeg.ninecards.googleplay.TestConfig._
import com.fortysevendeg.ninecards.googleplay.service.free.interpreter.TestData._
import com.fortysevendeg.ninecards.googleplay.util.WithHttp1Client
import org.specs2.matcher.TaskMatchers
import org.specs2.mutable.Specification

class GoogleApiClientIntegration extends Specification with WithHttp1Client {

  import TaskMatchers._

  private val apiServices = new ApiServices( new ApiClient(googleApiConf, pooledClient) )

  sequential

  "The ApiClient, the client to Google Play unofficial API" should {

    "Making an API request for an Item" should {
      "retrieve an Item for packages that exist" in {
        val response = apiServices.getItem(AppRequest(fisherPrice.packageObj,authParams))
        val fetchedDocId = response.map { xor => xor.map { item => (
          item.docV2.docid,
          item.docV2.title,
          item.docV2.details.appDetails.appCategory
        )}}
        val expected = {
          import fisherPrice.card
          (card.packageName, card.title, card.categories.take(1) )
        }
        fetchedDocId must returnValue( Xor.Right(expected) )
        // todo should this be more comprehensive? check all other tests too
      }

      "result in an error state for packages that do not exist" in {
        val appRequest = AppRequest(nonexisting.packageObj, authParams )
        apiServices.getItem(appRequest) must returnValue(Xor.left(nonexisting.packageName))
      }
    }

    "Making an API request for a Card" should {

      "result in an Item for packages that exist" in {
        def eraseDetails( card: AppCard) : AppCard = card.copy(
          downloads = "",
          categories = card.categories.take(1),
          stars = 3.145
        )
        val appRequest = AppRequest(fisherPrice.packageObj, authParams )
        val response = apiServices.getCard(appRequest)
        val fields = response.map( _.map(eraseDetails))
        // The number of downloads can be different from the Google API.
        fields must returnValue( Xor.Right( eraseDetails(fisherPrice.card)))
      }

      "result in an error state for packages that do not exist" in {
        val appRequest = AppRequest(nonexisting.packageObj, authParams)
        apiServices.getCard(appRequest) must returnValue(Xor.left(nonexisting.infoError))
      }
    }

  }

}