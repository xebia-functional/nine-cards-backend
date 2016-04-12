package com.fortysevendeg.ninecards.processes.converters

import java.sql.Timestamp

import com.fortysevendeg.ninecards.processes.domain._
import com.fortysevendeg.ninecards.processes.messages.InstallationsMessages._
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.messages.UserMessages.LoginResponse
import com.fortysevendeg.ninecards.services.common.NineCardsConfig.defaultConfig
import com.fortysevendeg.ninecards.services.free.domain.{
GooglePlayApp => GooglePlayAppServices,
Installation => InstallationServices,
SharedCollection => SharedCollectionServices,
SharedCollectionPackage => SharedCollectionPackageServices,
User => UserAppServices}
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.{
SharedCollectionData => SharedCollectionDataServices}
import org.http4s.Uri
import org.joda.time.DateTime

object Converters {

  implicit def toJodaDateTime(timestamp: Timestamp): DateTime = new DateTime(timestamp.getTime)

  implicit def toTimestamp(datetime: DateTime): Timestamp = new Timestamp(datetime.getMillis)

  def generateSharedCollectionLink(publicIdentifier: String) =
    Uri.fromString(defaultConfig.getString("ninecards.sharedCollectionsBaseUrl")) map { uri =>
      uri./(publicIdentifier).renderString
    } fold(
      error => "", //TODO: Maybe we should return an error if the url is malformed
      uri => uri)

  def toGooglePlayApp(app: GooglePlayAppServices): GooglePlayApp =
    GooglePlayApp(
      packageName = app.packageName,
      appType = app.appType,
      appCategory = app.appCategory,
      numDownloads = app.numDownloads,
      starRating = app.starRating,
      ratingCount = app.ratingCount,
      commentCount = app.commentCount)

  def toLoginResponse(info: (UserAppServices, InstallationServices)): LoginResponse = {
    val (user, _) = info
    LoginResponse(
      apiKey = user.apiKey,
      sessionToken = user.sessionToken)
  }

  def toGetCollectionByPublicIdentifierResponse(
    collection: SharedCollectionServices,
    packages: List[SharedCollectionPackageServices]): GetCollectionByPublicIdentifierResponse =
    GetCollectionByPublicIdentifierResponse(
      toSharedCollectionInfo(collection, packages map (_.packageName)))

  def toCreateCollectionResponse(
    collection: SharedCollectionServices,
    packages: List[String]): CreateCollectionResponse =
    CreateCollectionResponse(
      toSharedCollectionInfo(collection, packages))

  def toUpdateInstallationResponse(installation: InstallationServices): UpdateInstallationResponse =
    UpdateInstallationResponse(
      androidId = installation.androidId,
      deviceToken = installation.deviceToken)

  implicit def toSharedCollectionDataServices(
    publicIdentifier: String,
    publishedOn: DateTime,
    data: SharedCollectionData): SharedCollectionDataServices =
    SharedCollectionDataServices(
      publicIdentifier = publicIdentifier,
      userId = data.userId,
      publishedOn = publishedOn,
      description = data.description,
      author = data.author,
      name = data.name,
      installations = data.installations.getOrElse(0),
      views = data.views.getOrElse(0),
      category = data.category,
      icon = data.icon,
      community = data.community)

  def toSharedCollectionInfo(
    collection: SharedCollectionServices,
    packages: List[String]): SharedCollectionInfo =
    SharedCollectionInfo(
      publicIdentifier = collection.publicIdentifier,
      publishedOn = collection.publishedOn,
      description = collection.description,
      author = collection.author,
      name = collection.name,
      sharedLink = generateSharedCollectionLink(collection.publicIdentifier),
      installations = collection.installations,
      views = collection.views,
      category = collection.category,
      icon = collection.icon,
      community = collection.community,
      packages = packages,
      resolvedPackages = packages map (p => //TODO: Get the information about packages from Google
        ResolvedPackageInfo(
          packageName = p,
          title = "Title of the app",
          description = "Description of the app",
          free = true,
          icon = "url-to-the-icon",
          stars = 4.5,
          downloads = "100.000.000+")))
}
