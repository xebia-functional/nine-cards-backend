import sbt._

object NineCardsBuild extends Build with Settings with Dependencies {

  lazy val root = project.in(file(".")) aggregate(api, processes, services)

  lazy val services = project.in(file("modules/services"))
    .settings(projectSettings ++ servicesDeps)

  lazy val processes = project.in(file("modules/processes"))
    .settings(projectSettings)
    .dependsOn(services)

  lazy val api = project.in(file("modules/api"))
    .dependsOn(processes)
    .settings(apiSettings ++ apiDeps)
}