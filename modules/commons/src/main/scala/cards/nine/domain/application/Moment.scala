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
package cards.nine.domain.application

import enumeratum.{ Enum, EnumEntry }

sealed trait Moment extends EnumEntry

object Moment extends Enum[Moment] {

  case object CAR extends Moment
  case object HOME extends Moment
  case object MUSIC extends Moment
  case object NIGHT extends Moment
  case object OUT_AND_ABOUT extends Moment
  case object SPORT extends Moment
  case object STUDY extends Moment
  case object WORK extends Moment

  val values = super.findValues

  val valuesName = values.toList map (_.entryName)

  val widgetMomentPrefix = "WIDGET_"

  val widgetValuesName = values.toList map (v ⇒ s"$widgetMomentPrefix${v.entryName}")

  def isMoment(categoryName: String): Boolean =
    Moment.withNameOption(categoryName.replace(widgetMomentPrefix, "")).fold(false)(values.contains)
}