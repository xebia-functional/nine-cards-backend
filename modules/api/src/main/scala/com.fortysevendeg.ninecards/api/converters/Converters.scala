package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
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

  implicit def toApiCreateCollectionResponse(response: CreateCollectionResponse): ApiCreateCollectionResponse =
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
    collectionInfo: NewSharedCollectionInfo,
    userContext: UserContext
  ): CreateCollectionRequest =
    CreateCollectionRequest(
      collection = SharedCollectionData(
        publicIdentifier = collectionInfo.identifier.value,
        userId           = Option(userContext.userId.value),
        publishedOn      = collectionInfo.currentDate.value,
        description      = request.description,
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

  def toApiGetCollectionByPublicIdentifierResponse(r: GetCollectionByPublicIdentifierResponse) =
    ApiGetCollectionByPublicIdentifierResponse(
      publicIdentifier = r.data.publicIdentifier,
      publishedOn      = r.data.publishedOn,
      description      = r.data.description,
      author           = r.data.author,
      name             = r.data.name,
      sharedLink       = r.data.sharedLink,
      installations    = r.data.installations,
      views            = r.data.views,
      category         = r.data.category,
      icon             = r.data.icon,
      community        = r.data.community,
      packages         = r.data.packages,
      resolvedPackages = r.data.resolvedPackages map toApiResolvedPackageInfo
    )

  def toApiGetCollectionByPublicIdentifierResponse(
    response: XorGetCollectionByPublicId
  ): XorApiGetCollectionByPublicId =
    response map toApiGetCollectionByPublicIdentifierResponse

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

}
