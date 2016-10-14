package cards.nine.domain.account

/**
  * An AndroidId is a number, written as a hexadecimal 64-bit
  *  (16 characters) string, which uniquely identifies an Android device.
  */
case class AndroidId(value: String) extends AnyVal

