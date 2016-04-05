package com.fortysevendeg.ninecards.processes.converters

import java.sql.Timestamp

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.LoginResponse
import com.fortysevendeg.ninecards.services.free.domain.{
  GooglePlayApp ⇒ GooglePlayAppServices,
  Installation ⇒ InstallationServices,
  SharedCollection ⇒ SharedCollectionServices,
  SharedCollectionPackage ⇒ SharedCollectionPackageServices,
  User ⇒ UserAppServices
}
import org.joda.time.DateTime

object Converters {

  implicit def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  def toGooglePlayApp(app: GooglePlayAppServices): GooglePlayApp =
    GooglePlayApp(
      packageName  = app.packageName,
      appType      = app.appType,
      appCategory  = app.appCategory,
      numDownloads = app.numDownloads,
      starRating   = app.starRating,
      ratingCount  = app.ratingCount,
      commentCount = app.commentCount
    )

  def toLoginResponse(info: (UserAppServices, InstallationServices)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      apiKey       = user.apiKey,
      sessionToken = user.sessionToken
    )
  }

  def toGetCollectionByPublicIdentifierResponse(
    collection: SharedCollectionServices,
    packages:   List[SharedCollectionPackageServices]
  ): GetCollectionByPublicIdentifierResponse =
    GetCollectionByPublicIdentifierResponse(
      SharedCollectionInfo(
        publicIdentifier = collection.publicIdentifier,
        publishedOn      = collection.publishedOn,
        description      = collection.description,
        author           = collection.author,
        name             = collection.name,
        sharedLink       = "",
        installations    = collection.installations,
        views            = collection.views,
        category         = collection.category,
        icon             = collection.icon,
        community        = collection.community,
        packages         = packages map (_.packageName),
        resolvedPackages = packages map (p ⇒ //TODO: Get the information about packages from Google
        ResolvedPackageInfo(
          packageName = p.packageName,
          title       = "Title of the app",
          description = "Description of the app",
          free        = true,
          icon        = "url-to-the-icon",
          stars       = 4.5,
          downloads   = "100.000.000+"
        ))
      )
    )

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId   = installation.androidId,
      deviceToken = installation.deviceToken
    )

}
