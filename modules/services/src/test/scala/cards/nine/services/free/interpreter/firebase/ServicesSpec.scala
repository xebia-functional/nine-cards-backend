package cards.nine.services.free.interpreter.firebase

import cards.nine.commons.config.Domain.{ GoogleFirebaseConfiguration, GoogleFirebasePaths }
import cards.nine.commons.NineCardsErrors.NineCardsError
import cards.nine.domain.account.DeviceToken
import cards.nine.domain.application.Package
import cards.nine.services.free.domain.Firebase._
import cards.nine.services.utils.MockServerService
import org.mockserver.model.HttpRequest.{ request ⇒ mockRequest }
import org.mockserver.model.HttpResponse.{ response ⇒ mockResponse }
import org.mockserver.model.HttpStatusCode._
import org.mockserver.model.{ Header, JsonBody }
import org.specs2.matcher.{ DisjunctionMatchers, Matchers, XorMatchers }
import org.specs2.mutable.Specification

trait MockServer extends MockServerService {

  import TestData._

  override val mockServerPort = 9996

  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.sendNotification)
      .withHeader(headers.contentType)
      .withHeader(headers.authorization(auth.valid_token))
      .withBody(new JsonBody(requestBody))
  ).respond(
      mockResponse
        .withStatusCode(OK_200.code)
        .withHeader(jsonHeader)
        .withBody(new JsonBody(sendNotificationResponse))
    )

  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.sendNotification)
      .withHeader(headers.contentType)
      .withHeader(headers.authorization(auth.valid_token))
      .withBody(new JsonBody(invalidDeviceTokenRequestBody))
  ).respond(
      mockResponse
        .withStatusCode(OK_200.code)
        .withHeader(jsonHeader)
        .withBody(new JsonBody(invalidDeviceTokenSendNotificationResponse))
    )

  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.sendNotification)
      .withHeader(headers.contentType)
      .withHeader(headers.authorization(auth.invalid_token))
      .withBody(new JsonBody(requestBody))
  ).respond(
      mockResponse
        .withStatusCode(UNAUTHORIZED_401.code)
    )

  mockServer.when(
    mockRequest
      .withMethod("POST")
      .withPath(paths.sendNotification)
      .withHeader(headers.contentType)
      .withHeader(headers.authorization(auth.valid_token))
      .withBody(new JsonBody(badRequestBody))
  ).respond(
      mockResponse
        .withStatusCode(BAD_REQUEST_400.code)
        .withHeader(jsonHeader)
        .withBody(new JsonBody(badrequest_error))
    )
}

class ServicesSpec
  extends Specification
  with MockServer
  with Matchers
  with DisjunctionMatchers
  with XorMatchers {

  import TestData._

  val configuration = GoogleFirebaseConfiguration(
    protocol         = "http",
    host             = "localhost",
    port             = Option(mockServerPort),
    authorizationKey = auth.valid_token,
    paths            = GoogleFirebasePaths(paths.sendNotification)
  )

  val services = Services.services(configuration)

  "sendUpdatedCollectionNotification" should {

    "respond 200 OK and return a NotificationResponse object if a valid info is provided" in {
      val info = UpdatedCollectionNotificationInfo(
        deviceTokens     = List(auth.registrationId1, auth.registrationId2) map DeviceToken,
        publicIdentifier = content.collectionPublicIdentifier,
        packagesName     = List(packages.package1, packages.package2, packages.package3) map Package
      )

      val response = services.sendUpdatedCollectionNotification(info).unsafePerformSync

      response should beRight[SendNotificationResponse].which {
        info ⇒
          info.success must_== 2
          info.failure must_== 0
      }
    }

    "respond 200 OK and return a NotificationResponse object with errors if an invalid device " +
      "token is provided" in {
        val info = UpdatedCollectionNotificationInfo(
          deviceTokens     = List(auth.registrationId1, auth.registrationId2, auth.registrationId3) map DeviceToken,
          publicIdentifier = content.collectionPublicIdentifier,
          packagesName     = List(packages.package1, packages.package2, packages.package3) map Package
        )

        val response = services.sendUpdatedCollectionNotification(info).unsafePerformSync

        response should beRight[SendNotificationResponse].which { info ⇒
          info.failure must be_>(0)
        }
      }

    "respond 401 Unauthorized and return a FirebaseError value if an invalid auth token is provided" in {
      val badConfiguration: GoogleFirebaseConfiguration = configuration.copy(
        authorizationKey = auth.invalid_token
      )

      val services = Services.services(badConfiguration)

      val info = UpdatedCollectionNotificationInfo(
        deviceTokens     = List(auth.registrationId1, auth.registrationId2) map DeviceToken,
        publicIdentifier = content.collectionPublicIdentifier,
        packagesName     = List(packages.package1, packages.package2, packages.package3) map Package
      )

      val response = services.sendUpdatedCollectionNotification(info).unsafePerformSync

      response should beLeft[NineCardsError]
    }

    "respond 400 Bad Request and return a FirebaseError value if an empty list of device tokens is provided" in {
      val info = UpdatedCollectionNotificationInfo(
        deviceTokens     = List.empty,
        publicIdentifier = content.collectionPublicIdentifier,
        packagesName     = List(packages.package1, packages.package2, packages.package3) map Package
      )

      val response = services.sendUpdatedCollectionNotification(info).unsafePerformSync

      response should beRight[SendNotificationResponse](SendNotificationResponse.emptyResponse)
    }
  }
}

object TestData {

  val requestBody: String =
    s"""
       |{
       |  "registration_ids": [
       |    ${auth.registrationId1},
       |    ${auth.registrationId2}
       |  ],
       |  "data": {
       |    "payloadType": "sharedCollection",
       |    "payload": {
       |      "publicIdentifier": "${content.collectionPublicIdentifier}",
       |      "addedPackages":[
       |        ${packages.package1},
       |        ${packages.package2},
       |        ${packages.package3}
       |      ]
       |    }
       |  }
       |}
     """.stripMargin

  val invalidDeviceTokenRequestBody: String =
    s"""
       |{
       |  "registration_ids": [
       |    ${auth.registrationId1},
       |    ${auth.registrationId2},
       |    ${auth.registrationId3}
       |  ],
       |  "data": {
       |    "payloadType": "sharedCollection",
       |    "payload": {
       |      "publicIdentifier": "${content.collectionPublicIdentifier}",
       |      "addedPackages":[
       |        ${packages.package1},
       |        ${packages.package2},
       |        ${packages.package3}
       |      ]
       |    }
       |  }
       |}
     """.stripMargin

  val badRequestBody: String =
    s"""
       |{
       |  "registration_ids": [],
       |  "data": {
       |    "payloadType": "sharedCollection",
       |    "payload": {
       |      "publicIdentifier": "${content.collectionPublicIdentifier}",
       |      "addedPackages":[
       |        ${packages.package1},
       |        ${packages.package2},
       |        ${packages.package3}
       |      ]
       |    }
       |  }
       |}
     """.stripMargin

  val sendNotificationResponse =
    s"""
       |{
       |  "multicast_id": ${content.multicastId},
       |  "success": 2,
       |  "failure": 0,
       |  "canonical_ids": 0,
       |  "results":
       |  [
       |    {
       |      "message_id": "0:1472813701437453%8da4a5f5f9fd7ecd"
       |    },
       |    {
       |      "message_id": "0:1472813701440924%8da4a5f5f9fd7ecd"
       |    }
       |  ]
       |}
    """.stripMargin

  val invalidDeviceTokenSendNotificationResponse =
    s"""
       |{
       |  "multicast_id": ${content.multicastId},
       |  "success": 2,
       |  "failure": 1,
       |  "canonical_ids": 0,
       |  "results":
       |  [
       |    {
       |      "message_id": "${content.messageId1}"
       |    },
       |    {
       |      "message_id": "${content.messageId2}"
       |    },
       |    {
       |      "error": "The device is not registered"
       |    }
       |  ]
       |}
    """.stripMargin

  val badrequest_error = """"registration_ids" field cannot be empty""".stripMargin

  object auth {
    val valid_token = "granted"
    val invalid_token = "denied"

    val registrationId1 = "db540a04-80b7-4ec3-bfc1-5860e163dc7e"
    val registrationId2 = "68108c68-15fe-4731-801a-6861f22bb6af"
    val registrationId3 = "5720d031-0088-43c6-9035-6e18feba4efc"
  }

  object content {
    val collectionPublicIdentifier = "64490e0e-9207-4ac7-a9be-dc04be10b924"

    val messageId1 = "463cd5bd-0b16-4f2a-9cc2-433ddb185585"
    val messageId2 = "c151a16c-25b4-4284-8fbb-734517e271a6"

    val multicastId = 7546940006347645179l
  }

  object headers {
    def authorization(token: String) = new Header("Authorization", s"key=$token")

    val contentType = new Header("Content-Type", "application/json")
  }

  object packages {
    val package1 = "earth.europe.unitedKingdom"
    val package2 = "earth.europe.germany"
    val package3 = "earth.europe.france"
  }

  object paths {
    val sendNotification = "/fcm/send"
  }

}
