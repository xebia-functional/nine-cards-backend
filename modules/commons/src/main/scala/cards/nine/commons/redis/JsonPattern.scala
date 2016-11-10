package cards.nine.commons.redis

sealed trait JsonPattern

case object PStar extends JsonPattern

case object PNull extends JsonPattern

case class PString(value: String) extends JsonPattern

case class PObject(fields: List[(PString, JsonPattern)]) extends JsonPattern

object JsonPattern {

  def print(pattern: JsonPattern): String = pattern match {
    case PStar ⇒ """ * """.trim
    case PNull ⇒ """ null """.trim
    case PString(str) ⇒ s""" "$str" """.trim
    case PObject(fields) ⇒
      def printField(field: (PString, JsonPattern)): String = {
        val key = print(field._1)
        val value = print(field._2)
        s""" $key:$value """.trim
      }

      val fs = fields.map(printField).mkString(",")
      s""" {$fs} """.trim
  }

}
