package com.fortysevendeg.ninecards.processes

import java.sql.Timestamp
import java.time.Instant

import com.fortysevendeg.ninecards.processes.ProcessesExceptions.SharedCollectionNotFoundException
import com.fortysevendeg.ninecards.processes.messages.ApplicationMessages.AuthParams
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.services.free.domain.GooglePlay.{ AppInfo ⇒ AppInfoServices, AppsInfo }
import com.fortysevendeg.ninecards.services.free.domain.{ SharedCollection ⇒ SharedCollectionServices, SharedCollectionPackage, SharedCollectionSubscription }
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.{ SharedCollectionData ⇒ SharedCollectionDataServices }
import org.joda.time.DateTime

object TestData {

  val androidId = "50a4dbf7-85a2-4875-8c75-7232c237808c"

  val appCategory = "COUNTRY"

  val author = "John Doe"

  val category = "SOCIAL"

  val collectionId = 1l

  val community = true

  val description = Option("Description about the collection")

  val icon = "path-to-icon"

  val installations = 1

  val localization = Option("en-EN")

  val millis = 1453226400000l

  val name = "The best social media apps"

  val packagesSize = 0

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val publishedOnDatetime = new DateTime(millis)

  val publishedOnTimestamp = Timestamp.from(Instant.ofEpochMilli(millis))

  val publisherId = 27L

  val sharedLink = s"http://localhost:8080/collections/$publicIdentifier"

  val stars = 5.0d

  val subscriberId = 42L

  val token = "6d54dfed-bbcf-47a5-b8f2-d86cf3320631"

  val userId = Option(publisherId)

  val views = 1

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  )

  val missing = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom"
  )

  object Values {

    val apps = packagesName map { packageName ⇒
      AppInfoServices(packageName, "Germany", true, icon, stars, "100.000+", List(appCategory))
    }

    val appsInfo = AppsInfo(missing, apps)

    val authParams = AuthParams(
      androidId    = androidId,
      localization = localization,
      token        = token
    )

    val collection = SharedCollectionServices(
      id               = collectionId,
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnTimestamp,
      description      = description,
      author           = author,
      name             = name,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community
    )

    val nonExistentSharedCollection: Option[SharedCollectionServices] = None

    val packages = packagesName.zip(1l to packagesName.size.toLong) map {
      case (n, id) ⇒
        SharedCollectionPackage(id, collectionId, n)
    }

    val appInfoList = packagesName map { packageName ⇒
      AppInfo(packageName, "Germany", true, icon, stars, "100.000+", appCategory)
    }

    val sharedCollectionDataServices = SharedCollectionDataServices(
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnTimestamp,
      description      = description,
      author           = author,
      name             = name,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community
    )

    val sharedCollectionData = SharedCollectionData(
      publicIdentifier = publicIdentifier,
      userId           = userId,
      publishedOn      = publishedOnDatetime,
      description      = description,
      author           = author,
      name             = name,
      installations    = Option(installations),
      views            = Option(views),
      category         = category,
      icon             = icon,
      community        = community
    )

    val sharedCollection = SharedCollection(
      publicIdentifier = publicIdentifier,
      publishedOn      = new DateTime(publishedOnTimestamp.getTime),
      description      = description,
      author           = author,
      name             = name,
      sharedLink       = sharedLink,
      installations    = installations,
      views            = views,
      category         = category,
      icon             = icon,
      community        = community,
      packages         = packagesName
    )

    val sharedCollectionWithAppsInfo = SharedCollectionWithAppsInfo(
      collection = sharedCollection,
      appsInfo   = appInfoList
    )

    val subscription = SharedCollectionSubscription(
      id                 = 1L,
      sharedCollectionId = collectionId,
      userId             = subscriberId
    )
  }

  object Messages {

    import Values._

    val createCollectionRequest: CreateCollectionRequest = CreateCollectionRequest(
      collection = sharedCollectionData,
      packages   = packagesName
    )

    val createCollectionResponse = CreateCollectionResponse(data = sharedCollection)

    val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
      data = sharedCollectionWithAppsInfo
    )

  }

  object Exceptions {

    val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
      message = "The required shared collection doesn't exist"
    )
  }
}
