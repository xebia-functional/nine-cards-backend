import FlywayConfig._
import org.flywaydb.sbt.FlywayPlugin._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy._
import spray.revolver.RevolverPlugin

trait Settings {
  this: Build =>

  lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := Versions.scala,
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    organizationHomepage := Some(new URL("http://47deg.com")),
    version := Versions.buildVersion,
    conflictWarning := ConflictWarning.disable,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:higherKinds", "-language:implicitConversions", "-language:reflectiveCalls"),
    javaOptions in Test ++= Seq("-XX:MaxPermSize=128m", "-Xms512m", "-Xmx512m"),
    sbt.Keys.fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in(Test, packageSrc) := true,
    logLevel := Level.Info,
    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.defaultLocal,
      Classpaths.typesafeReleases,
      Resolver.bintrayRepo("scalaz", "releases"),
      DefaultMavenRepository,
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Flyway" at "https://flywaydb.org/repo"
    ),
    doc in Compile <<= target.map(_ / "none")
  )

  lazy val apiSettings = projectSettings ++
    Seq(
      databaseConfig := databaseConfigDef.value,
      run <<= run in Runtime dependsOn flywayMigrate
    ) ++
    RevolverPlugin.settings ++
    nineCardsFlywaySettings ++
    nineCardsAssemblySettings

  lazy val nineCardsFlywaySettings =
    Seq(flywaySettings: _*) ++
      Seq(
        flywayDriver := databaseConfig.value.driver,
        flywayUrl := databaseConfig.value.url,
        flywayUser := databaseConfig.value.user,
        flywayPassword := databaseConfig.value.password
      )

  lazy val nineCardsAssemblySettings = assemblySettings ++ Seq(
    assemblyJarName in assembly := s"9cards-${Versions.buildVersion}.jar",
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
    publishArtifact in(Test, packageBin) := false
  )

  lazy val serviceSettings = projectSettings ++ Seq(
    unmanagedClasspath in Test += (baseDirectory in LocalProject("api")).value / "src/main/resources"
  )
}