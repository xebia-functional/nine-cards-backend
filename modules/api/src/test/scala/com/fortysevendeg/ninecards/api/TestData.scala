package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.api.NineCardsHeaders._
import com.fortysevendeg.ninecards.api.messages.GooglePlayMessages.ApiCategorizeAppsRequest
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages.ApiUpdateInstallationRequest
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages.ApiCreateCollectionRequest
import com.fortysevendeg.ninecards.api.messages.UserMessages.ApiLoginRequest
import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.CategorizeAppsResponse
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.{ LoginRequest, LoginResponse }
import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import org.joda.time.DateTime
import spray.http.HttpHeaders.RawHeader

object TestData {

  val androidId = "f07a13984f6d116a"

  val apiToken = "a7db875d-f11e-4b0c-8d7a-db210fd93e1b"

  val author = "John Doe"

  val authToken = "c8abd539-d912-4eff-8d3c-679307defc71"

  val category = "SOCIAL"

  val community = true

  val description = Option("Description about the collection")

  val deviceToken = Option("d897b6f1-c6a9-42bd-bf42-c787883c7d3e")

  val email = "valid.email@test.com"

  val failingAuthToken = "a439c00e-9a01-4b0e-a446-1d8410229072"

  val googlePlayToken = "8d8f9814-d5cc-4e69-9225-517a5257e5b7"

  val icon = "path-to-icon"

  val installations = 1

  val marketLocalization = "en-us"

  val name = "The best social media apps"

  val now = DateTime.now

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val sessionToken = "1d1afeea-c7ec-45d8-a6f8-825b836f2785"

  val sharedLink = "link-to-shared-collection"

  val tokenId = "6c7b303e-585e-4fe8-8b6f-586547317331-7f9b12dd-8946-4285-a72a-746e482834dd"

  val userId = 1l

  val views = 1

  object Exceptions {

    val http4sException = org.http4s.InvalidResponseException(msg = "Test error")

    val persistenceException = PersistenceException(
      message = "Test error",
      cause   = None
    )

    val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
      message = "Test error",
      cause   = None
    )
  }

  object Headers {

    val userInfoHeaders = List(
      RawHeader(headerAndroidId, androidId),
      RawHeader(headerSessionToken, sessionToken),
      RawHeader(headerAuthToken, authToken)
    )

    val googlePlayHeaders = List(
      RawHeader(headerGooglePlayToken, googlePlayToken),
      RawHeader(headerMarketLocalization, marketLocalization)
    )

    val failingUserInfoHeaders = List(
      RawHeader(headerAndroidId, androidId),
      RawHeader(headerSessionToken, sessionToken),
      RawHeader(headerAuthToken, failingAuthToken)
    )
  }

  object Messages {

    val sharedCollection = SharedCollection(
      publicIdentifier = publicIdentifier,
      publishedOn      = now,
      description      = description,
      author           = author,
      name             = name,
      sharedLink       = sharedLink,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community,
      packages         = List.empty
    )

    val sharedCollectionInfo = SharedCollectionWithAppsInfo(
      collection = sharedCollection,
      appsInfo   = List.empty
    )

    val apiCategorizeAppsRequest = ApiCategorizeAppsRequest(items = List("", "", ""))

    val apiCreateCollectionRequest = ApiCreateCollectionRequest(
      description   = description,
      author        = author,
      name          = name,
      installations = Option(installations),
      views         = Option(views),
      category      = category,
      icon          = icon,
      community     = community,
      packages      = List.empty
    )

    val apiLoginRequest = ApiLoginRequest(email, androidId, tokenId)

    val apiUpdateInstallationRequest = ApiUpdateInstallationRequest(deviceToken)

    val categorizeAppsResponse = CategorizeAppsResponse(Nil, Nil)

    val createCollectionResponse = CreateCollectionResponse(data = sharedCollection)

    val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
      data = sharedCollectionInfo
    )

    val getPublishedCollectionsResponse = GetPublishedCollectionsResponse(Nil)

    val loginRequest = LoginRequest(email, androidId, sessionToken, tokenId)

    val loginResponse = LoginResponse(apiToken, sessionToken)

    val subscribeResponse = SubscribeResponse()

    val unsubscribeResponse = UnsubscribeResponse()

    val updateInstallationRequest = UpdateInstallationRequest(userId, androidId, deviceToken)

    val updateInstallationResponse = UpdateInstallationResponse(androidId, deviceToken)

  }

  object Paths {

    val apiDocs = "/apiDocs/index.html"

    val categorize = "/applications/categorize"

    val collections = "/collections"

    val collectionById = "/collections/40daf308-fecf-4228-9262-a712d783cf49"

    val installations = "/installations"

    val invalid = "/chalkyTown"

    val login = "/login"
  }

}
