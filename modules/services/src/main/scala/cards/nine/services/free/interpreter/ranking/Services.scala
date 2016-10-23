package cards.nine.services.free.interpreter.ranking

import cards.nine.commons.CacheWrapper
import cards.nine.domain.analytics.{ CountryScope, GeoScope, RankedApp, WorldScope }
import cards.nine.domain.application.{ Category, Moments, Package }
import cards.nine.googleplay.processes.withTypes.WithRedisClient
import cards.nine.services.free.algebra.Ranking._
import cards.nine.services.free.domain.Ranking._
import cats.~>
import cats.syntax.either._
import com.redis.RedisClient
import com.redis.serialization.{ Format, Parse }
import enumeratum.{ Circe ⇒ CirceEnum }
import io.circe.{ Decoder, Encoder }
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._

import scalaz.concurrent.Task

class Services(implicit
  format: Format,
  keyParse: Parse[Option[CacheKey]],
  valParse: Parse[Option[CacheVal]]) extends (Ops ~> WithRedisClient) {

  private[this] def generateCacheKey(scope: GeoScope) = scope match {
    case WorldScope ⇒ CacheKey.worldScope
    case CountryScope(code) ⇒ CacheKey.countryScope(code.value)
  }

  def apply[A](fa: Ops[A]): WithRedisClient[A] = fa match {
    case GetRanking(scope) ⇒ client: RedisClient ⇒
      Task.delay {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)

        val value = wrap.get(generateCacheKey(scope))

        value.flatMap(_.ranking).getOrElse(GoogleAnalyticsRanking(Map.empty))
      }

    case GetRankingForApps(scope, apps) ⇒ client: RedisClient ⇒
      Task.delay {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)

        val rankings = wrap.get(generateCacheKey(scope))
          .flatMap(_.ranking)
          .getOrElse(GoogleAnalyticsRanking(Map.empty))

        val rankingsByCategory = rankings.categories.filterNot {
          case (category, _) ⇒ Moments.isMoment(category)
        }

        val packagesByCategory = apps.toList.groupBy(_.category).mapValues(_.map(_.packageName))

        val rankedByCategory = rankingsByCategory flatMap {
          case (category, ranking) ⇒
            ranking
              .intersect(packagesByCategory.getOrElse(category, Nil))
              .zipWithIndex
              .map { case (pack, position) ⇒ RankedApp(pack, category, Option(position)) }
        }

        Either.right(rankedByCategory.toList)
      }

    case UpdateRanking(scope, ranking) ⇒ client: RedisClient ⇒
      Task.delay {
        val wrap = CacheWrapper[CacheKey, CacheVal](client)

        val key = scope match {
          case WorldScope ⇒ CacheKey.worldScope
          case CountryScope(code) ⇒ CacheKey.countryScope(code.value)
        }

        val value = CacheVal(Option(ranking))

        wrap.put((key, value))
        Either.right(UpdateRankingSummary(ranking.categories.values.size, 0))
      }
  }
}

object Services {

  implicit lazy val categoryD: Decoder[Category] = CirceEnum.decoder(Category)
  implicit lazy val categoryE: Encoder[Category] = CirceEnum.encoder(Category)

  implicit lazy val packageD: Decoder[Package] = Decoder.decodeString map Package
  implicit lazy val packageE: Encoder[Package] = Encoder.encodeString.contramap(_.value)

  implicit lazy val appRankingInfoD: Decoder[AppRankingInfo] = deriveDecoder[AppRankingInfo]
  implicit lazy val appRankingInfoE: Encoder[AppRankingInfo] = deriveEncoder[AppRankingInfo]

  implicit lazy val rankingD: Decoder[GoogleAnalyticsRanking] = deriveDecoder[GoogleAnalyticsRanking]
  implicit lazy val rankingE: Encoder[GoogleAnalyticsRanking] = deriveEncoder[GoogleAnalyticsRanking]

  implicit val keyParse: Parse[Option[CacheKey]] =
    Parse(bv ⇒ decode[CacheKey](Parse.Implicits.parseString(bv)).toOption)

  implicit val valParse: Parse[Option[CacheVal]] =
    Parse(bv ⇒ decode[CacheVal](Parse.Implicits.parseString(bv)).toOption)

  implicit def keyAndValFormat(implicit ek: Encoder[CacheKey], ev: Encoder[CacheVal]): Format =
    Format {
      case key: CacheKey ⇒ ek(key).noSpaces
      case value: CacheVal ⇒ ev(value).noSpaces
    }

  def services(implicit
    format: Format,
    keyParse: Parse[Option[CacheKey]],
    valParse: Parse[Option[CacheVal]]) = new Services()(keyAndValFormat, keyParse, valParse)
}
