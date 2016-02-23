package com.fortysevendeg.ninecards.googleplay

import cats.data.Xor
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.http.{ContentTypes, HttpEntity}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

import scalaz.concurrent.Task

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
    *  A to response marshaller capable of completing Scalaz concurrent tasks
    *  @param m A to response marshaller for the underlying type
    *  @return A marshaller capable of completing a Task
    */
  implicit def tasksMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[Task[A]] =
    ToResponseMarshaller[Task[A]] {
      (task, ctx) =>
        task.runAsync {
          _.fold(
            left => ctx.handleError(left),
            right => m(right, ctx))
        }
    }

}
