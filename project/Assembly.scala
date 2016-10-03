
trait Assembly9C {

  import com.typesafe.sbt.SbtNativePackager._
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport.scriptClasspath
  import sbt.Keys._
  import sbt._
  import sbtassembly.AssemblyPlugin._
  import sbtassembly.AssemblyPlugin.autoImport._
  import sbtassembly.MergeStrategy._

  final val nineCardsAssemblySettings = assemblySettings ++ Seq(
    assemblyJarName in assembly := s"9cards-google-play-${Versions.buildVersion}.jar",
    assembleArtifact in assemblyPackageScala := true,
    Keys.test in assembly := {},
    assemblyMergeStrategy in assembly := {
      case "application.conf" => concat
      case "reference.conf" => concat
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