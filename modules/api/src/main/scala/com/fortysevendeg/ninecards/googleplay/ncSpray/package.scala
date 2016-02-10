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
  * Some notes here
  */
package object ncSpray {

  implicit def circeJsonMarshaller[A](implicit encoder: Encoder[A]): Marshaller[A] = Marshaller.of[A](ContentTypes.`application/json`) {
    case (a, contentType, ctx) => ctx.marshalTo(HttpEntity(ContentTypes.`application/json`, encoder(a).noSpaces))
  }

  implicit def exceptionalXorMarshaller[A, E <: Exception](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Xor[E, A]] = {
    ToResponseMarshaller[Xor[E, A]] { (v, ctx) =>
      v.fold({ e =>
        ctx.marshalTo(HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity(e.toString)))
      }, m(_, ctx))
    }
  }
}
