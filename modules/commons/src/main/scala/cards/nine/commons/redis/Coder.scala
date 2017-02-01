/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cards.nine.commons.redis

import cats.syntax.either._
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
