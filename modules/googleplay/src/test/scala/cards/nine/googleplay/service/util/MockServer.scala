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
package cards.nine.googleplay.service.util

import org.mockserver.configuration.ConfigurationProperties
import org.mockserver.integration.ClientAndServer._
import org.specs2.specification.{ BeforeAfterEach, BeforeAfterAll }

trait MockServer extends BeforeAfterAll with BeforeAfterEach {

  val mockServerPort: Int = 9995

  protected[this] lazy val mockServer = {
    ConfigurationProperties.overrideLogLevel("ERROR")
    startClientAndServer(mockServerPort)
  }

  override def beforeAll = {}
  override def afterAll = mockServer.stop

  override def before = mockServer.reset()
  override def after = {}
}
