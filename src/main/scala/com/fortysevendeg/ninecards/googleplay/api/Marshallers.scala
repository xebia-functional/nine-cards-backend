package com.fortysevendeg.ninecards.googleplay.api

import cats.data.Xor
import com.fortysevendeg.ninecards.googleplay.domain._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Encoder, Decoder}
import scalaz.concurrent.Task
import spray.http.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller, ToResponseMarshallingContext}
import spray.httpx.unmarshalling.{MalformedContent, Unmarshaller}

/**
  * Some implicit conversions for using Circe and Cats with Spray.
  */
object NineCardsMarshallers {

  /**
    * A marshaller capable of writing any case class to JSON, using the structure of the case class.
    * @param encoder An encoder, which may be automatically derived using Circe's generic derivation
    * @return A Marshaller capable of writing the given type to JSON
    */
  implicit def circeJsonMarshaller[A](implicit encoder: Encoder[A]): Marshaller[A] =
    Marshaller.of[A](ContentTypes.`application/json`) {
      case (a, contentType, ctx) => ctx.marshalTo(HttpEntity(ContentTypes.`application/json`, encoder(a).noSpaces))
    }

  implicit val itemMarshaller: ToResponseMarshaller[Item] =
    circeJsonMarshaller(implicitly[Encoder[Item]])

  implicit val appCardMarshaller: ToResponseMarshaller[AppCard] =
    circeJsonMarshaller(implicitly[Encoder[AppCard]])

  // Domain-specific marshalling and unmarshalling
  implicit object packageListUnmarshaller extends Unmarshaller[PackageList] {
    def apply(entity: HttpEntity) = {
      decode[PackageList](entity.asString).fold(
        e => Left(MalformedContent(s"Unable to parse entity into JSON list: $entity", e)),
        s => Right(s)
      )
    }
  }

  implicit val optionalItemMarshaller: ToResponseMarshaller[Option[Item]] =
    ToResponseMarshaller[Option[Item]] { (o, ctx) =>
      val response = HttpResponse(
        status = StatusCodes.InternalServerError,
        entity = HttpEntity("Cannot find item!")
      )
      o.fold(ctx.marshalTo(response))(itemMarshaller(_, ctx))
    }

  implicit val xorAppMarshaller: ToResponseMarshaller[Xor[InfoError,AppCard]] =
    ToResponseMarshaller[Xor[InfoError,AppCard]] { (xor, ctx) => xor match {
      case Xor.Left(infoError) =>
        val im = implicitly[Marshaller[InfoError]]
        val response = HttpResponse(
          status = StatusCodes.NotFound,
          entity = spray.httpx.marshalling.marshalUnsafe(infoError)
        )
        ctx.marshalTo(response)
      case Xor.Right(appCard) =>
        val im = implicitly[ToResponseMarshaller[AppCard]]
        im(appCard,ctx)
    }
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
