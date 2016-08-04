resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.2.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.3")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
