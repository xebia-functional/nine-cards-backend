

object Scalariform9C {

  import com.typesafe.sbt.SbtScalariform
  import com.typesafe.sbt.SbtScalariform.ScalariformKeys
  import scalariform.formatter.preferences._

  lazy val settings = SbtScalariform.scalariformSettings ++ Seq(
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

