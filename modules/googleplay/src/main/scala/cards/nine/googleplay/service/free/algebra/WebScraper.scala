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
package cards.nine.googleplay.service.free.algebra

import cats.free.{ Free, Inject }
import cards.nine.domain.application.{ FullCard, Package }
import cards.nine.googleplay.domain.webscrapper._

object WebScraper {

  sealed trait Ops[A]

  case class ExistsApp(pack: Package) extends Ops[Boolean]

  case class GetDetails(pack: Package) extends Ops[Failure Either FullCard]

  class Service[F[_]](implicit i: Inject[Ops, F]) {

    def existsApp(pack: Package): Free[F, Boolean] =
      Free.inject[Ops, F](ExistsApp(pack))

    def getDetails(pack: Package): Free[F, Failure Either FullCard] =
      Free.inject[Ops, F](GetDetails(pack))
  }

  object Service {
    implicit def service[F[_]](implicit I: Inject[Ops, F]): Service[F] = new Service[F]
  }

}
