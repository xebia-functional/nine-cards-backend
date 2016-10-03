import sbt._

object NineCardsBuild extends Build with Settings {

  lazy val root = googleplay

  lazy val googleplay = project.in(file("modules/googleplay"))
    .settings(googleplaySettings ++ Dependencies.googleplayDeps)

}
