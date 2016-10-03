
trait Settings {

  import sbt.Keys._
  import sbt._

  lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % Versions.kindProjector ),
    name := "9cards-backend-google-play",
    scalaVersion := Versions.scala,
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    organizationHomepage := Some(new URL("http://47deg.com")),
    version := Versions.buildVersion,
    conflictWarning := ConflictWarning.disable,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:higherKinds", "-Ywarn-unused-import"),
    javaOptions in Test ++= Seq("-XX:MaxPermSize=128m", "-Xms512m", "-Xmx512m"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in(Test, packageSrc) := true,
    logLevel := Level.Info,
    parallelExecution in Test := false,
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.defaultLocal,
      Classpaths.typesafeReleases,
      Resolver.bintrayRepo("scalaz", "releases"),
      DefaultMavenRepository,
      "Sonatype Snapshots"  at "https://oss.sonatype.org/content/repositories/snapshots"
    ),
    doc in Compile <<= target.map(_ / "none")
  )

  val googleplaySettings = {
    val protoBufSettings = {
      import sbtprotobuf.{ProtobufPlugin => PB}
      PB.protobufSettings ++ Seq(
        PB.runProtoc in PB.protobufConfig := { args =>
          com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)
        }
      )
    }

    projectSettings ++ Seq( publishArtifact in(Test, packageBin) := false ) ++ protoBufSettings

  }

}
