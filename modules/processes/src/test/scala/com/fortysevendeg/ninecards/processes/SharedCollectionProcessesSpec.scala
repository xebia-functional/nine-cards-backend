package com.fortysevendeg.ninecards.processes

import java.sql.Timestamp
import java.time.Instant

import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.processes.ProcessesExceptions._
import com.fortysevendeg.ninecards.processes.converters.Converters
import com.fortysevendeg.ninecards.processes.messages.SharedCollectionMessages._
import com.fortysevendeg.ninecards.processes.utils.{ DummyNineCardsConfig, XorMatchers }
import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.{SharedCollectionData => SharedCollectionDataServices}
import com.fortysevendeg.ninecards.services.persistence._
import doobie.imports._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.Matchers.{eq => mockEq}
import org.specs2.ScalaCheck
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.Scalaz._

trait SharedCollectionProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with DummyNineCardsConfig
  with SharedCollectionProcessesContext
  with XorMatchers {

  trait BasicScope extends Scope {

    implicit val sharedCollectionPersistenceServices = mock[SharedCollectionPersistenceServices]
    implicit val sharedCollectionProcesses = new SharedCollectionProcesses[NineCardsServices]

  }

  trait SharedCollectionSuccessfulScope extends BasicScope {

    sharedCollectionPersistenceServices.addCollection[SharedCollection](
      data = mockEq(sharedCollectionDataServices))(any) returns collection.point[ConnectionIO]

    sharedCollectionPersistenceServices.addPackages(
      collectionId = collectionId,
      packagesName = packagesName) returns packagesSize.point[ConnectionIO]

    sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
      publicIdentifier = publicIdentifier
    ) returns Option(collection).point[ConnectionIO]

    sharedCollectionPersistenceServices.getPackagesByCollection(
      collectionId = collectionId
    ) returns List.empty[SharedCollectionPackage].point[ConnectionIO]
  }

  trait SharedCollectionUnsuccessfulScope extends BasicScope {

    sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
      publicIdentifier = publicIdentifier) returns nonExistentSharedCollection.point[ConnectionIO]
  }

}

trait SharedCollectionProcessesContext {

  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"

  val collectionId = 1l

  val userId = None

  val millis = 1453226400000l

  val publishedOnTimestamp = Timestamp.from(Instant.ofEpochMilli(millis))

  val publishedOnDatetime = new DateTime(millis)

  val description = Option("Description about the collection")

  val author = "John Doe"

  val name = "The best social media apps"

  val installations = 1

  val views = 1

  val category = "SOCIAL"

  val icon = "path-to-icon"

  val community = true

  val sharedLink = s"http://localhost:8080/collections/$publicIdentifier"

  val packagesName = List.empty[String]

  val packagesSize = 0

  val collection = SharedCollection(
    id               = collectionId,
    publicIdentifier = publicIdentifier,
    userId = userId,
    publishedOn = publishedOnTimestamp,
    description = description,
    author = author,
    name = name,
    installations = installations,
    views = views,
    category = category,
    icon = icon,
    community = community)

  val nonExistentSharedCollection: Option[SharedCollection] = None

  val sharedCollectionDataServices = SharedCollectionDataServices(
    publicIdentifier = publicIdentifier,
    userId = userId,
    publishedOn = publishedOnTimestamp,
    description = description,
    author = author,
    name = name,
    installations = installations,
    views = views,
    category = category,
    icon = icon,
    community = community)

  val sharedCollectionData = SharedCollectionData(
    publicIdentifier = publicIdentifier,
    userId = userId,
    publishedOn = publishedOnDatetime,
    description = description,
    author = author,
    name = name,
    installations = Option(installations),
    views = Option(views),
    category = category,
    icon = icon,
    community = community)

  val sharedCollectionInfo = SharedCollectionInfo(
    publicIdentifier = publicIdentifier,
    publishedOn = new DateTime(publishedOnTimestamp.getTime),
    description = description,
    author = author,
    name = name,
    sharedLink = sharedLink,
    installations = installations,
    views = views,
    category = category,
    icon = icon,
    community = community,
    packages = List.empty,
    resolvedPackages = List.empty)

  val createCollectionRequest: CreateCollectionRequest = CreateCollectionRequest(
    collection = sharedCollectionData,
    packages = List.empty)

  val createCollectionResponse = CreateCollectionResponse(data = sharedCollectionInfo)

  val getCollectionByPublicIdentifierResponse = GetCollectionByPublicIdentifierResponse(
    data = sharedCollectionInfo
  )

  val sharedCollectionNotFoundException = SharedCollectionNotFoundException(
    message = "The required shared collection doesn't exist"
  )
}

class SharedCollectionProcessesSpec
  extends SharedCollectionProcessesSpecification
  with ScalaCheck {

  "createCollection" should {
    "return a valid response info when the shared collection is created" in
      new SharedCollectionSuccessfulScope {
        val response = sharedCollectionProcesses.createCollection(
          request = createCollectionRequest)

        response.foldMap(testInterpreters) must_== createCollectionResponse
      }
  }

  "getCollectionByPublicIdentifier" should {
    "return a valid shared collection info when the shared collection exists" in
      new SharedCollectionSuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          publicIdentifier = publicIdentifier
        )

        collectionInfo.foldMap(testInterpreters) must beXorRight(getCollectionByPublicIdentifierResponse)
      }

    "return a SharedCollectionNotFoundException when the shared collection doesn't exist" in
      new SharedCollectionUnsuccessfulScope {
        val collectionInfo = sharedCollectionProcesses.getCollectionByPublicIdentifier(
          publicIdentifier = publicIdentifier
        )

        collectionInfo.foldMap(testInterpreters) must beXorLeft(sharedCollectionNotFoundException)
      }
  }
}
