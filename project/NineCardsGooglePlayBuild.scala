import sbt._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging

object NineCardsBuild extends Build with Settings {

  lazy val root = googleplay

  lazy val googleplay = project.in(file("modules/googleplay"))
    .enablePlugins(JavaAppPackaging)
    .settings(apiSettings ++ Dependencies.apiDeps)


}
