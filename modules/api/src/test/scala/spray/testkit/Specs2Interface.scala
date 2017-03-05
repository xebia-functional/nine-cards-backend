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
package akka.http.scaladsl.testkit

import org.specs2.execute.{ Failure, FailureException }
import org.specs2.specification.core.{ Fragments, SpecificationStructure }
import org.specs2.specification.create.DefaultFragmentFactory

trait Specs2Interface extends TestFrameworkInterface with SpecificationStructure {

  def failTest(msg: String) = {
    val trace = new Exception().getStackTrace.toList
    val fixedTrace = trace.drop(trace.indexWhere(_.getClassName.startsWith("org.specs2")) - 1)
    throw new FailureException(Failure(msg, stackTrace = fixedTrace))
  }

  override def map(fs: ⇒ Fragments) = super.map(fs).append(DefaultFragmentFactory.step(cleanUp()))
}

trait NoAutoHtmlLinkFragments extends org.specs2.specification.dsl.ReferenceDsl {
  override def linkFragment(alias: String) = super.linkFragment(alias)
  override def seeFragment(alias: String) = super.seeFragment(alias)
}