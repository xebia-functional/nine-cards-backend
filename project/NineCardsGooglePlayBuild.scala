import sbt._
import Keys._

import sbtprotobuf.{ProtobufPlugin=>PB}

object NineCardsBuild extends Build with Settings {

  lazy val root = project.in(file("."))
    .settings(apiSettings ++ Dependencies.apiDeps ++ PB.protobufSettings : _*)
}
