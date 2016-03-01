import sbt._
import Keys._

import sbtprotobuf.{ProtobufPlugin=>PB}

object NineCardsBuild extends Build with Settings with Dependencies {

  lazy val root = project.in(file("."))
    .settings(apiSettings ++ apiDeps ++ PB.protobufSettings : _*)
}
