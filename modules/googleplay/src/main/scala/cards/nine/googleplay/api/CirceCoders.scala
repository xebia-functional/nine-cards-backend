package cards.nine.googleplay.api

import cards.nine.googleplay.domain._
import io.circe.{Decoder, Encoder}

object CirceCoders {

  import io.circe.generic.auto._
  import io.circe.generic.semiauto._

  implicit val packageD: Decoder[Package] = Decoder.decodeString.map(Package.apply)
  implicit val packageE: Encoder[Package] = Encoder.encodeString.contramap( _.value)

  implicit val packageListD: Decoder[PackageList] = deriveDecoder[PackageList]
  implicit val packageListE: Encoder[PackageList] = deriveEncoder[PackageList]

  implicit val packageDetailsD: Decoder[PackageDetails] = deriveDecoder[PackageDetails]
  implicit val packageDetailsE: Encoder[PackageDetails] = deriveEncoder[PackageDetails]

  implicit val apiCardD: Decoder[ApiCard] = deriveDecoder[ApiCard]
  implicit val apiCardE: Encoder[ApiCard] = deriveEncoder[ApiCard]

  implicit val apiCardListD: Decoder[ApiCardList] = deriveDecoder[ApiCardList]
  implicit val apiCardListE: Encoder[ApiCardList] = deriveEncoder[ApiCardList]

  implicit val infoErrorD: Decoder[InfoError] = deriveDecoder[InfoError]
  implicit val infoErrorE: Encoder[InfoError] = deriveEncoder[InfoError]

  implicit val itemD: Decoder[Item] = deriveDecoder[Item]
  implicit val itemE: Encoder[Item] = deriveEncoder[Item]

  implicit val apiRecommendByAppD: Decoder[ApiRecommendByAppsRequest] =
    deriveDecoder[ApiRecommendByAppsRequest]

  implicit val apiRecommendByAppE: Encoder[ApiRecommendByAppsRequest] =
    deriveEncoder[ApiRecommendByAppsRequest]

  implicit val apiRecommendByCategoryD: Decoder[ApiRecommendByCategoryRequest] =
    deriveDecoder[ApiRecommendByCategoryRequest]

  implicit val apiRecommendByCategoryE: Encoder[ApiRecommendByCategoryRequest] =
    deriveEncoder[ApiRecommendByCategoryRequest]

  implicit val apiRecommendationListE: Encoder[ApiRecommendationList] =
    deriveEncoder[ApiRecommendationList]

  implicit val apiRecommendationListD: Decoder[ApiRecommendationList] =
    deriveDecoder[ApiRecommendationList]

}