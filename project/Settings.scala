import CustomSettings._
import org.flywaydb.sbt.FlywayPlugin._
import org.flywaydb.sbt.FlywayPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy._
import spray.revolver.RevolverPlugin

trait Settings {
  this: Build =>

  lazy val scalariformSettings = {
    import com.typesafe.sbt.SbtScalariform
    import com.typesafe.sbt.SbtScalariform.ScalariformKeys

    import scalariform.formatter.preferences._

    SbtScalariform.scalariformSettings ++ Seq(
      SbtScalariform.ScalariformKeys.preferences := ScalariformKeys.preferences.value
        .setPreference(SpacesWithinPatternBinders, true)
        .setPreference(SpaceBeforeColon, false)
        .setPreference(SpaceInsideParentheses, false)
        .setPreference(SpaceInsideBrackets, false)
        .setPreference(SpacesAroundMultiImports, true)
        .setPreference(PreserveSpaceBeforeArguments, false)
        .setPreference(CompactStringConcatenation, false)
        //.setPreference(NewlineAtEndOfFile, false)
        .setPreference(DanglingCloseParenthesis, Force)
        .setPreference(CompactControlReadability, false)
        .setPreference(AlignParameters, false)
        .setPreference(AlignArguments, true)
        .setPreference(AlignSingleLineCaseStatements, false)
        .setPreference(DoubleIndentClassDeclaration, false)
        //.setPreference(DoubleIndentMethodDeclaration, true)
        .setPreference(IndentLocalDefs, false)
        .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
        .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
        .setPreference(RewriteArrowSymbols, true)
    )
  }

  lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    addCompilerPlugin("org.spire-math" %% "kind-projector" % Versions.kindProjector),
    scalaVersion := Versions.scala,
    organization := "com.fortysevendeg",
    organizationName := "47 Degrees",
    organizationHomepage := Some(new URL("http://47deg.com")),
    version := Versions.buildVersion,
    conflictWarning := ConflictWarning.disable,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:higherKinds", "-language:implicitConversions"),
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
      "Flyway" at "https://flywaydb.org/repo",
      "RoundEights" at "http://maven.spikemark.net/roundeights"
    ),
    doc in Compile <<= target.map(_ / "none")
  ) ++ scalariformSettings

  lazy val apiSettings = projectSettings ++
    Seq(
      databaseConfig := databaseConfigDef.value,
      apiResourcesFolder := apiResourcesFolderDef.value,
      run <<= run in Runtime dependsOn flywayMigrate
    ) ++
    RevolverPlugin.settings ++
    nineCardsFlywaySettings ++
    nineCardsAssemblySettings

  lazy val nineCardsFlywaySettings = flywayBaseSettings(Runtime) ++ Seq(
    flywayDriver := databaseConfig.value.driver,
    flywayUrl := databaseConfig.value.url,
    flywayUser := databaseConfig.value.user,
    flywayPassword := databaseConfig.value.password,
    flywayLocations := Seq("filesystem:" + apiResourcesFolder.value.getPath + "/db/migration")
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

  lazy val processesSettings = projectSettings ++ Seq(
    apiResourcesFolder := apiResourcesFolderDef.value,
    unmanagedClasspath in Test += apiResourcesFolder.value
  )

  lazy val serviceSettings = projectSettings ++ Seq(
    apiResourcesFolder := apiResourcesFolderDef.value,
    unmanagedClasspath in Test += apiResourcesFolder.value
  )
}