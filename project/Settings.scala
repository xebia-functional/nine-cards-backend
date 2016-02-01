import FlywayConfig._
import org.flywaydb.sbt.FlywayPlugin._
import sbt.Keys._
import sbt._
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
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
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
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),
    doc in Compile <<= target.map(_ / "none")
  )

  lazy val apiSettings = projectSettings ++ Seq(
    databaseConfig := databaseConfigDef.value,
    run <<= run in Runtime dependsOn flywayMigrate,
    publishArtifact in(Test, packageBin) := false
  ) ++ RevolverPlugin.settings ++ Seq(flywaySettings: _*) ++ Seq(
    flywayDriver := databaseConfig.value.driver,
    flywayUrl := databaseConfig.value.url,
    flywayUser := databaseConfig.value.user,
    flywayPassword := databaseConfig.value.password
  )

}