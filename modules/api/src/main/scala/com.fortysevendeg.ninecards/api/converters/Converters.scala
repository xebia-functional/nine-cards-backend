package com.fortysevendeg.ninecards.api.converters

import com.fortysevendeg.ninecards.api.NineCardsHeaders.Domain._
import com.fortysevendeg.ninecards.api.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.api.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.api.messages.UserMessages._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages._

object Converters {

  implicit def toLoginRequest(
    request: ApiLoginRequest
  ): LoginRequest =
    LoginRequest(
      email     = request.email,
      androidId = request.androidId,
      tokenId   = request.tokenId
    )

  implicit def toApiLoginResponse(
    response: LoginResponse
  ): ApiLoginResponse =
    ApiLoginResponse(
      apiKey       = response.apiKey,
      sessionToken = response.sessionToken
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

  implicit def toApiGetCollectionByPublicIdentifierResponse(
    response: XorGetCollectionByPublicId
  ): XorApiGetCollectionByPublicId =
    response map (r ⇒
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
      ))

  implicit def toUpdateInstallationRequest(
    request: ApiUpdateInstallationRequest
  )(
    implicit
    userContext: UserContext
  ): UpdateInstallationRequest =
    UpdateInstallationRequest(
      userId      = userContext.userId.value,
      androidId   = userContext.androidId.value,
      deviceToken = request.deviceToken
    )

  implicit def toApiUpdateInstallationResponse(
    response: UpdateInstallationResponse
  ): ApiUpdateInstallationResponse =
    ApiUpdateInstallationResponse(
      androidId   = response.androidId,
      deviceToken = response.deviceToken
    )

}
