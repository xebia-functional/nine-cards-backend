resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.2.1")
addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.5.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
libraryDependencies += "com.github.os72" % "protoc-jar" % "3.0.0-b4"