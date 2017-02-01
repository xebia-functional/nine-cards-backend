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
import sbt._

object NineCardsBuild extends Build with ApiSettings {
  import Dependencies._
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import org.flywaydb.sbt.FlywayPlugin

  lazy val root = project.in(file("."))
    .disablePlugins(FlywayPlugin)
    .aggregate(api, commons, processes, services, googleplay)

  lazy val api = project.in(file("modules/api"))
    .enablePlugins(JavaAppPackaging)
    .settings(apiSettings ++ apiDeps)
    .dependsOn(processes, commons)

  lazy val commons = project.in(file("modules/commons"))
    .disablePlugins(FlywayPlugin)
    .settings(projectSettings ++ commonsDeps)

  lazy val googleplay = project.in(file("modules/googleplay"))
    .disablePlugins(FlywayPlugin)
    .settings(googleplaySettings ++ googleplayDeps)
    .dependsOn(commons % "compile -> compile; test -> test" )

  lazy val services = project.in(file("modules/services"))
    .disablePlugins(FlywayPlugin)
    .settings(serviceSettings ++ servicesDeps)
    .dependsOn(googleplay, commons % "compile -> compile; test -> test" )

  lazy val processes = project.in(file("modules/processes"))
    .disablePlugins(FlywayPlugin)
    .settings(processesSettings ++ processesDeps)
    .dependsOn(services, commons)

  lazy val tests = Project(id = "tests", base = file("modules/tests"))
    .disablePlugins(FlywayPlugin)
    .settings(projectSettings: _*)
    .aggregate(api, commons, processes, services, googleplay)
}