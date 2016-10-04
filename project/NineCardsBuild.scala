import sbt._

object NineCardsBuild extends Build with ApiSettings {
  import Dependencies._
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import org.flywaydb.sbt.FlywayPlugin

  lazy val root = project.in(file("."))
    .disablePlugins(FlywayPlugin)
    .aggregate(api, processes, services)

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
    .dependsOn(commons)

  lazy val services = project.in(file("modules/services"))
    .disablePlugins(FlywayPlugin)
    .settings(serviceSettings ++ servicesDeps)
    .dependsOn(commons)

  lazy val processes = project.in(file("modules/processes"))
    .disablePlugins(FlywayPlugin)
    .settings(processesSettings ++ processesDeps)
    .dependsOn(services, commons)

  lazy val tests = Project(id = "tests", base = file("modules/tests"))
    .disablePlugins(FlywayPlugin)
    .settings(projectSettings: _*)
    .aggregate(api, processes, services, api)
}