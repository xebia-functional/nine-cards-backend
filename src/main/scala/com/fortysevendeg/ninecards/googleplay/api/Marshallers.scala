package com.fortysevendeg.ninecards.googleplay.api

import cats.free.Free
import cats.data.Xor
import cats.{ ~>, Id, Monad}
import com.fortysevendeg.ninecards.googleplay.domain._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.parser._
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

  implicit def xorAppMarshaller[A](implicit im: ToResponseMarshaller[A]): ToResponseMarshaller[InfoError Xor A] =
    ToResponseMarshaller[InfoError Xor A] { (xor, ctx) => xor match {
      case Xor.Right(a) =>
        im(a,ctx)
      case Xor.Left(infoError) =>
        val im = implicitly[Marshaller[InfoError]]
        val response = HttpResponse(
          status = StatusCodes.NotFound,
          entity = spray.httpx.marshalling.marshalUnsafe(infoError)
        )
        ctx.marshalTo(response)
    }
  }

  implicit val appRecommendationListMarshaller: ToResponseMarshaller[AppRecommendationList] =
    circeJsonMarshaller(implicitly[Encoder[AppRecommendationList]])

  /**
    *  A to response marshaller capable of completing Scalaz concurrent tasks
    *  @param m A to response marshaller for the underlying type
    *  @return A marshaller capable of completing a Task
    */
  private[this] type TRM[A] = ToResponseMarshaller[A]

  trait TRMFactory[F[_]] {
    implicit def makeTRM[A](implicit ma: TRM[A]): TRM[F[A]]
  }

  class TaskMarshaller[A](m: TRM[A]) extends TRM[Task[A]] {
    override def apply( task: Task[A], ctx: ToResponseMarshallingContext) =
      task.runAsync {
        _.fold(
          left => ctx.handleError(left),
          right => m(right, ctx))
      }
  }

  implicit object TaskMarshallerFactory extends TRMFactory[Task] {
    override implicit def makeTRM[A](implicit ma: TRM[A]) : TRM[Task[A]] =
      new TaskMarshaller[A](ma)
  }

  class IdMarshaller[A](base: TRM[A]) extends TRM[Id[A]] {
    override def apply( idA: Id[A], ctx: ToResponseMarshallingContext) = base.apply(idA, ctx)
  }

  implicit object IdMarshallerFactory extends TRMFactory[Id] {
    override implicit def makeTRM[A](implicit ma: TRM[A]) : TRM[Id[A]] =
      new IdMarshaller[A](ma)
  }

  class ContraNaturalTransformFreeTRMFactory[ F[_], G[_] ](
    implicit
      interpreter: F ~> G,
    gMonad: Monad[G],
    factory: TRMFactory[G]
      /* currified types and partial application, it would be `Free[F]` */
  ) extends TRMFactory[ ({type L[A] = Free[F, A]})#L ] {

    override implicit def makeTRM[A](implicit ma: TRM[A]) : TRM[Free[F,A]] = {
      def fa2ga(fa: Free[F,A]): G[A] = fa.foldMap(interpreter)
      factory.makeTRM[A](ma).compose(fa2ga)
    }

  }

  implicit def contraNaturalTransformFreeTRMFactory[ F[_], G[_] ](
    implicit interpreter: F ~> G, gMonad: Monad[G], factory: TRMFactory[G]
  ) : TRMFactory[ ({type L[A] = Free[F, A]})#L ] =
    new ContraNaturalTransformFreeTRMFactory[F,G]()(interpreter, gMonad, factory)


}
