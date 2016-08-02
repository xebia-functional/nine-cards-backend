import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._

trait Settings {
  this: Build =>

  lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    name := "9Cards-backend-google-play",
    scalaVersion := Versions.scala,
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    organizationHomepage := Some(new URL("http://47deg.com")),
    version := Versions.buildVersion,
    conflictWarning := ConflictWarning.disable,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:higherKinds", "-Ywarn-unused-import"),
    javaOptions in Test ++= Seq("-XX:MaxPermSize=128m", "-Xms512m", "-Xmx512m"),
    sbt.Keys.fork in Test := false,
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

  lazy val apiSettings = projectSettings ++ Seq(
    publishArtifact in(Test, packageBin) := false
  ) ++ RevolverPlugin.settings ++ nineCardsAssemblySettings

  lazy val nineCardsAssemblySettings = assemblySettings ++ Seq(
    assemblyJarName in assembly := s"9cards-google-play-${Versions.buildVersion}.jar",
    assembleArtifact in assemblyPackageScala := true,
    Keys.test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case "application.conf" => concat
      case "reference.conf" => concat
      case "unwanted.txt" => discard
      case entry =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        val mergeStrategy = oldStrategy(entry)
        mergeStrategy == deduplicate match {
          case true => first
          case _ => mergeStrategy
        }
    },
    publishArtifact in(Test, packageBin) := false,
    mappings in Universal <<= (mappings in Universal, assembly in Compile) map { (mappings, fatJar) =>
      val filtered = mappings filter { case (file, fileName) => !fileName.endsWith(".jar") }
      filtered :+ (fatJar -> ("lib/" + fatJar.getName))
    },
    scriptClasspath := Seq((assemblyJarName in assembly).value)
  )
}
