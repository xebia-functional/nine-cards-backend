import sbt._

object NineCardsBuild extends Build with Settings with Dependencies {

  lazy val root = project.in(file(".")) aggregate(api, processes, services)

  lazy val services = project.in(file("modules/services"))
    .settings(serviceSettings ++ servicesDeps)

  lazy val processes = project.in(file("modules/processes"))
    .settings(projectSettings ++ processesDeps)
    .dependsOn(services)

  lazy val api = project.in(file("modules/api"))
    .dependsOn(processes)
    .settings(apiSettings ++ apiDeps)

  lazy val commons = Project(id = "commons", base = file("modules/commons"))
    .settings(projectSettings ++ commonDeps)

  lazy val tests = Project(id = "tests", base = file("modules/tests"))
    .settings(projectSettings: _*)
    .aggregate(api, processes, services, api)
}