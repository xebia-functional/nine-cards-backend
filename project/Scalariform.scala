/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

