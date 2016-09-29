import sbt._

object NineCardsBuild extends Build with ApiSettings {
  import Dependencies._
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import org.flywaydb.sbt.FlywayPlugin

  lazy val root = project.in(file("."))
    .disablePlugins(FlywayPlugin)
    .aggregate(api, processes, services)

  lazy val services = project.in(file("modules/services"))
    .disablePlugins(FlywayPlugin)
    .settings(serviceSettings ++ servicesDeps)

  lazy val processes = project.in(file("modules/processes"))
    .disablePlugins(FlywayPlugin)
    .settings(processesSettings ++ processesDeps)
    .dependsOn(services)

  lazy val api = project.in(file("modules/api"))
    .enablePlugins(JavaAppPackaging)
    .settings(apiSettings ++ apiDeps)
    .dependsOn(processes)

  lazy val tests = Project(id = "tests", base = file("modules/tests"))
    .disablePlugins(FlywayPlugin)
    .settings(projectSettings: _*)
    .aggregate(api, processes, services, api)
}