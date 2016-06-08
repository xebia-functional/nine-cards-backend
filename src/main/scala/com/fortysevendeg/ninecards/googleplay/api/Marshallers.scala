package com.fortysevendeg.ninecards.googleplay.api

import com.fortysevendeg.ninecards.googleplay.domain.Domain.{Item, PackageListRequest}
import io.circe.{Encoder, Decoder}
import scalaz.concurrent.Task
import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller, ToResponseMarshallingContext}
import spray.httpx.unmarshalling.{MalformedContent, Unmarshaller}

/**
  * Some implicit conversions for using Circe and Cats with Spray.
  */
object NineCardsMarshallers {

  // Domain-specific marshalling and unmarshalling
  implicit object packageListUnmarshaller extends Unmarshaller[PackageListRequest] {
    import io.circe.generic.auto._
    import io.circe.parser._

    def apply(entity: HttpEntity) = {
      decode[PackageListRequest](entity.asString).fold(
        e => Left(MalformedContent(s"Unable to parse entity into JSON list: $entity", e)),
        s => Right(s)
      )
    }
  }

  implicit def optionalItemMarshaller(implicit im: ToResponseMarshaller[Item]): ToResponseMarshaller[Option[Item]] = {
    ToResponseMarshaller[Option[Item]] { (o, ctx) =>
      val response = HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("Cannot find item!"))
      o.fold(ctx.marshalTo(response))(im(_, ctx))
    }
  }

  /**
    * A marshaller capable of writing any case class to JSON, using the structure of the case class.
    * @param encoder An encoder, which may be automatically derived using Circe's generic derivation
    * @return A Marshaller capable of writing the given type to JSON
    */
  implicit def circeJsonMarshaller[A](implicit encoder: Encoder[A]): Marshaller[A] =
    Marshaller.of[A](ContentTypes.`application/json`) {
      case (a, contentType, ctx) => ctx.marshalTo(HttpEntity(ContentTypes.`application/json`, encoder(a).noSpaces))
    }

  /**
    *  A to response marshaller capable of completing Scalaz concurrent tasks
    *  @param m A to response marshaller for the underlying type
    *  @return A marshaller capable of completing a Task
    */
  implicit def tasksMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Task[A]] =
    ToResponseMarshaller[Task[A]] { (task, ctx) =>
      task.runAsync {
        _.fold(
          left => ctx.handleError(left),
          right => m(right, ctx))
      }
    }

}
