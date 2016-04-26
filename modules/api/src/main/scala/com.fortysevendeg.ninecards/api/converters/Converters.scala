package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._

object Converters {

  def toLoginRequest(request: ApiLoginRequest): LoginRequest =
    LoginRequest(
      email     = request.email,
      androidId = request.androidId,
      tokenId   = request.tokenId
    )

  def toApiLoginResponse(response: LoginResponse): ApiLoginResponse =
    ApiLoginResponse(
      apiKey       = response.apiKey,
      sessionToken = response.sessionToken
    )

  def toApiCreateCollectionResponse(
    response: CreateCollectionResponse
  ): ApiCreateCollectionResponse =
    ApiCreateCollectionResponse(
      publicIdentifier = response.data.publicIdentifier,
      publishedOn      = response.data.publishedOn,
      description      = response.data.description,
      author           = response.data.author,
      name             = response.data.name,
      sharedLink       = response.data.sharedLink,
      installations    = response.data.installations,
      views            = response.data.views,
      category         = response.data.category,
      icon             = response.data.icon,
      community        = response.data.community,
      packages         = response.data.packages
    )

  def toCreateCollectionRequest(
    request: ApiCreateCollectionRequest,
    userContext: UserContext
  ): CreateCollectionRequest =
    CreateCollectionRequest(
      collection = SharedCollectionData(
        userId        = Option(userContext.userId.value),
        description   = request.description,
        author        = request.author,
        name          = request.name,
        installations = request.installations,
        views         = request.views,
        category      = request.category,
        icon          = request.icon,
        community     = request.community
      ),
      packages   = request.packages
    )

  def toApiResolvedPackageInfo(resolvedPackageInfo: ResolvedPackageInfo) =
    ApiResolvedPackageInfo(
      packageName = resolvedPackageInfo.packageName,
      title       = resolvedPackageInfo.title,
      description = resolvedPackageInfo.description,
      free        = resolvedPackageInfo.free,
      icon        = resolvedPackageInfo.icon,
      stars       = resolvedPackageInfo.stars,
      downloads   = resolvedPackageInfo.downloads
    )

  @inline
  def toApiSharedCollection(info: SharedCollectionInfo): ApiSharedCollection =
    ApiSharedCollection(
      publicIdentifier = info.publicIdentifier,
      publishedOn      = info.publishedOn,
      description      = info.description,
      author           = info.author,
      name             = info.name,
      sharedLink       = info.sharedLink,
      installations    = info.installations,
      views            = info.views,
      category         = info.category,
      icon             = info.icon,
      community        = info.community,
      packages         = info.packages,
      resolvedPackages = info.resolvedPackages map toApiResolvedPackageInfo
    )

  @inline
  def toXorApiSharedCollection(
    response: XorGetCollectionByPublicId
  ): XorApiGetCollectionByPublicId =
    response map (r ⇒ toApiSharedCollection(r.data))

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

  def toApiSubscribeResponse(_x: SubscribeResponse): ApiSubscribeResponse =
    ApiSubscribeResponse()

  def toApiXorSubscribeResponse(response: XorSubscribeResponse): XorApiSubscribeResponse =
    response map toApiSubscribeResponse

  def toApiUnsubscribeResponse(_x: UnsubscribeResponse): ApiUnsubscribeResponse =
    ApiUnsubscribeResponse()

  def toApiXorUnsubscribeResponse(response: XorUnsubscribeResponse): XorApiUnsubscribeResponse =
    response map toApiUnsubscribeResponse

  @inline
  def toApiSharedCollectionList(response: GetPublishedCollectionsResponse): List[ApiSharedCollection] =
    response.collections map toApiSharedCollection

}
