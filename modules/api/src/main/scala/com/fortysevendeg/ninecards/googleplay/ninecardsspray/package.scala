package com.fortysevendeg.ninecards.googleplay

import cats.data.Xor
import spray.httpx.marshalling.Marshaller
import spray.httpx.marshalling.ToResponseMarshaller
import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import com.fortysevendeg.ninecards.api.Domain.PackageListRequest

import spray.httpx.unmarshalling.MalformedContent
import spray.httpx.unmarshalling.Unmarshaller
import spray.http.HttpEntity

/**
  * Some implicit conversions for using Circe and Cats with Spray.
  */
package object ninecardsspray {

  /**
    * A marshaller capable of writing any case class to JSON, using the structure of the case class.
    * @param encoder An encoder, which may be automatically derived using Circe's generic derivation
    * @return A Marshaller capable of writing the given type to JSON
    */
  implicit def circeJsonMarshaller[A](implicit encoder: Encoder[A]): Marshaller[A] = Marshaller.of[A](ContentTypes.`application/json`) {
    case (a, contentType, ctx) => ctx.marshalTo(HttpEntity(ContentTypes.`application/json`, encoder(a).noSpaces))
  }

  /**
    * A response marshaller capable of handling Cats Xor, with the error type being an Exception
    * @param m a ToResponseMarshaller for the successful type. It is usually enough to have an implicit marshaller for that type. See circeJsonMarshaller.
    * @return a ToResponseMarshaller capable of handling error cases automatically.
    */
/*  implicit def exceptionalXorMarshaller[A, E <: Exception](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Xor[E, A]] = {
    ToResponseMarshaller[Xor[E, A]] { (v, ctx) =>
      v.fold({ e =>
        ctx.marshalTo(HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity(e.toString)))
      }, m(_, ctx))
    }
  }
 */}
