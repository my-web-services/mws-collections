name := "macros_project"

scalaVersion in ThisBuild := "2.11.8"

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  resolvers +=
    Resolver.url("scalameta", url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns)
  ,
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0.132" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet
)

lazy val macros = project.settings(
  metaMacroSettings,
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.3.0"
)

lazy val macros_project = (project in file("."))
  .settings(metaMacroSettings)
  .aggregate(macros)
  .dependsOn(macros)

//mainClass in (Compile,run) := Some("Main")

fork in run := false
