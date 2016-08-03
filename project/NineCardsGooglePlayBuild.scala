import sbt._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

object NineCardsBuild extends Build with Settings {

  lazy val root = project.in(file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(apiSettings ++ Dependencies.apiDeps)
}
