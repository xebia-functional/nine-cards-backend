package cards.nine.commons.redis

import io.circe.{ Decoder, Encoder }
import io.circe.parser._
import scredis.serialization.{ Reader, Writer, UTF8StringReader, UTF8StringWriter }

object Readers {

  def parser[A](parser: String ⇒ Option[A]): Reader[Option[A]] =
    new ParseStringReader[A](parser)

  def decoder[A](decoder: Decoder[A]): Reader[Option[A]] =
    new CirceDecoderReader[A](decoder)

}

object Writers {

  def printer[A](printer: A ⇒ String): Writer[A] = new PrinterReader[A](printer)

  def encoder[A](encoder: Encoder[A]): Writer[A] = new CirceEncoderWriter[A](encoder)

}

class ParseStringReader[A](val parse: String ⇒ Option[A]) extends Reader[Option[A]] {

  override protected def readImpl(bytes: Array[Byte]): Option[A] =
    parse(UTF8StringReader.read(bytes))

}

class PrinterReader[A](val print: A ⇒ String) extends Writer[A] {

  override protected def writeImpl(elem: A): Array[Byte] =
    UTF8StringWriter.write(print(elem))

}

class CirceDecoderReader[A](val decoder: Decoder[A]) extends Reader[Option[A]] {

  override protected def readImpl(bytes: Array[Byte]): Option[A] =
    decode[A](UTF8StringReader.read(bytes))(decoder).toOption

}

class CirceEncoderWriter[A](val encoder: Encoder[A]) extends Writer[A] {

  override protected def writeImpl(elem: A): Array[Byte] =
    UTF8StringWriter.write(encoder(elem).noSpaces)

}
