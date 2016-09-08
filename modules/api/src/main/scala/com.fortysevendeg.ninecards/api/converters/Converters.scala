package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.messages.GooglePlayMessages.{ ApiCategorizeAppsResponse, ApiDetailAppsResponse, CategorizedApp }
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._

object Converters {

  def toLoginRequest(request: ApiLoginRequest, sessionToken: SessionToken): LoginRequest =
    LoginRequest(
      email        = request.email,
      androidId    = request.androidId,
      sessionToken = sessionToken.value,
      tokenId      = request.tokenId
    )

  implicit def toApiLoginResponse(response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      apiKey       = response.apiKey,
      sessionToken = response.sessionToken
    )

  implicit def toApiCreateOrUpdateCollectionResponse(
    response: CreateOrUpdateCollectionResponse
  ): ApiCreateOrUpdateCollectionResponse =
    ApiCreateOrUpdateCollectionResponse(
      publicIdentifier = response.publicIdentifier,
      packagesStats    = response.packagesStats
    )

  def toCreateCollectionRequest(
    request: ApiCreateCollectionRequest,
    collectionInfo: NewSharedCollectionInfo,
    userContext: UserContext
  ): CreateCollectionRequest =
    CreateCollectionRequest(
      collection = SharedCollectionData(
        publicIdentifier = collectionInfo.identifier.value,
        userId           = Option(userContext.userId.value),
        publishedOn      = collectionInfo.currentDate.value,
        author           = request.author,
        name             = request.name,
        installations    = request.installations,
        views            = request.views,
        category         = request.category,
        icon             = request.icon,
        community        = request.community
      ),
      packages   = request.packages
    )

  @inline
  def toApiSharedCollection(info: SharedCollectionWithAppsInfo): ApiSharedCollection =
    ApiSharedCollection(
      publicIdentifier = info.collection.publicIdentifier,
      publishedOn      = info.collection.publishedOn,
      author           = info.collection.author,
      name             = info.collection.name,
      installations    = info.collection.installations,
      views            = info.collection.views,
      category         = info.collection.category,
      icon             = info.collection.icon,
      community        = info.collection.community,
      packages         = info.collection.packages,
      appsInfo         = info.appsInfo,
      subscriptions    = info.collection.subscriptionsCount
    )

  def toApiSharedCollectionList(response: GetCollectionsResponse): ApiSharedCollectionList =
    ApiSharedCollectionList(response.collections map toApiSharedCollection)

  def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest,
    userContext: UserContext
  ): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId      = userContext.userId.value,
      androidId   = userContext.androidId.value,
      deviceToken = request.deviceToken
    )

  def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse
  ): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId   = response.androidId,
      deviceToken = response.deviceToken
    )

  def toApiSubscribeResponse(response: SubscribeResponse): ApiSubscribeResponse =
    ApiSubscribeResponse()

  def toApiUnsubscribeResponse(response: UnsubscribeResponse): ApiUnsubscribeResponse =
    ApiUnsubscribeResponse()

  def toAuthParams(googlePlayContext: GooglePlayContext, userContext: UserContext) =
    AuthParams(
      androidId    = userContext.androidId.value,
      localization = googlePlayContext.marketLocalization map (_.value),
      token        = googlePlayContext.googlePlayToken.value
    )

  def toCategorizedApp(appInfo: AppGooglePlayInfo): CategorizedApp =
    CategorizedApp(
      packageName = appInfo.packageName,
      category    = appInfo.categories.headOption getOrElse ""
    )

  def toApiCategorizeAppsResponse(response: GetAppsInfoResponse): ApiCategorizeAppsResponse =
    ApiCategorizeAppsResponse(
      items  = response.items map toCategorizedApp,
      errors = response.errors
    )

  def toApiDetailAppsResponse(response: GetAppsInfoResponse): ApiDetailAppsResponse =
    ApiDetailAppsResponse(
      items  = response.items,
      errors = response.errors
    )

  def toApiGetSubscriptionsByUser(response: GetSubscriptionsByUserResponse): ApiGetSubscriptionsByUser =
    ApiGetSubscriptionsByUser(
      subscriptions = response.subscriptions
    )
}
