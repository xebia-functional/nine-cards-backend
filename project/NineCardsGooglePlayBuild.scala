import sbt._
import Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbtprotobuf.{ProtobufPlugin => PB}

object NineCardsBuild extends Build with Settings {

  lazy val root = project.in(file("."))
    .enablePlugins(JavaAppPackaging)
    .settings(apiSettings ++ Dependencies.apiDeps ++ PB.protobufSettings : _*)
}
