name := "youi"
organization in ThisBuild := "io.youi"
version in ThisBuild := "0.4.7"
scalaVersion in ThisBuild := "2.12.2"
crossScalaVersions in ThisBuild := List("2.12.2", "2.11.11")
resolvers in ThisBuild += Resolver.sonatypeRepo("releases")
resolvers in ThisBuild += Resolver.sonatypeRepo("snapshots")
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

val profigVersion = "1.0.1"
val pixiJsVersion = "4.5.3"
val scribeVersion = "1.4.3"
val powerScalaVersion = "2.0.5"
val reactifyVersion = "2.0.3"
val akkaVersion = "2.5.2"
val scalaJSDOM = "0.9.3"
val httpAsyncClientVersion = "4.1.3"
val circeVersion = "0.8.0"
val uaDetectorVersion = "2014.10"
val undertowVersion = "1.4.18.Final"
val uPickleVersion = "0.4.4"
val closureCompilerVersion = "v20170423"
val hasherVersion = "1.2.1"
val canvgVersion = "1.4.0_1"
val openTypeVersion = "0.7.1_2"
val picaVersion = "3.0.5"
val scalaXMLVersion = "1.0.6"
val scalacticVersion = "3.0.3"
val scalaTestVersion = "3.0.3"

lazy val root = project.in(file("."))
  .aggregate(
    coreJS, coreJVM, stream, communicationJS, communicationJVM, dom, client, server, serverUndertow, ui, optimizer,
    appJS, appJVM, templateJS, templateJVM, exampleJS, exampleJVM
  )
  .settings(
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    publish := {},
    publishLocal := {}
  )

lazy val core = crossProject.in(file("core"))
  .settings(
    name := "youi-core",
    description := "Core functionality leveraged and shared by most other sub-projects of YouI.",
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.outr" %%% "profig" % profigVersion,
      "com.outr" %%% "scribe" % scribeVersion,
      "com.outr" %%% "reactify" % reactifyVersion,
      "org.scalactic" %%% "scalactic" % scalacticVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test"
    ),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser"
    ).map(_ % circeVersion)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % scalaJSDOM
    )
  )

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val stream = project.in(file("stream"))
  .settings(
    name := "youi-stream",
    libraryDependencies ++= Seq(
      "org.powerscala" %% "powerscala-io" % powerScalaVersion
    )
  )
  .dependsOn(coreJVM)

lazy val dom = project.in(file("dom"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "youi-dom"
  )
  .dependsOn(coreJS)
  .dependsOn(stream % "compile")

lazy val client = project.in(file("client"))
  .settings(
    name := "youi-client",
    libraryDependencies ++= Seq(
      "org.apache.httpcomponents" % "httpasyncclient" % httpAsyncClientVersion,
      "org.powerscala" %% "powerscala-io" % powerScalaVersion
    )
  )
  .dependsOn(coreJVM)

lazy val server = project.in(file("server"))
  .settings(
    name := "youi-server",
    libraryDependencies ++= Seq(
      "net.sf.uadetector" % "uadetector-resources" % uaDetectorVersion,
      "org.scalactic" %% "scalactic" % scalacticVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  )
  .dependsOn(coreJVM, stream)

lazy val serverUndertow = project.in(file("serverUndertow"))
  .settings(
    name := "youi-server-undertow",
    libraryDependencies ++= Seq(
      "io.undertow" % "undertow-core" % undertowVersion,
      "org.scalactic" %% "scalactic" % scalacticVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  )
  .dependsOn(server)

lazy val communication = crossProject.in(file("communication"))
  .settings(
    name := "youi-communication",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % uPickleVersion,
      "org.scalactic" %%% "scalactic" % scalacticVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test"
    )
  )
  .dependsOn(core)

lazy val communicationJS = communication.js
lazy val communicationJVM = communication.jvm.dependsOn(server)

lazy val ui = project.in(file("ui"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "youi-ui",
    test := (),
    libraryDependencies ++= Seq(
      "com.outr" %%% "scalajs-pixijs" % pixiJsVersion,
      "com.outr" %%% "canvg-scala-js" % canvgVersion,
      "com.outr" %%% "opentype-scala-js" % openTypeVersion,
      "com.outr" %%% "pica-scala-js" % picaVersion
    )
  )
  .dependsOn(coreJS, dom)

lazy val optimizer = project.in(file("optimizer"))
  .settings(
    name := "youi-optimizer",
    description := "Provides optimization functionality for application development.",
    fork := true,
    libraryDependencies ++= Seq(
      "com.google.javascript" % "closure-compiler" % closureCompilerVersion,
      "org.powerscala" %% "powerscala-io" % powerScalaVersion,
      "com.outr" %% "scribe" % scribeVersion,
      "com.outr" %% "hasher" % hasherVersion
    )
  )
  .dependsOn(stream)

lazy val app = crossProject.in(file("app"))
  .settings(
    name := "youi-app",
    libraryDependencies ++= Seq(
      "org.scalactic" %%% "scalactic" % scalacticVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test"
    )
  )
  .jsSettings(
    test := ()
  )
  .dependsOn(core, communication)

lazy val appJS = app.js.dependsOn(ui)
lazy val appJVM = app.jvm

lazy val template = crossProject.in(file("template"))
  .settings(
    name := "youi-template"
  )
  .jsSettings(
    test := (),
    crossTarget in fastOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    crossTarget in fullOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    crossTarget in packageJSDependencies := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    skip in packageJSDependencies := false
  )
  .jvmSettings(
    fork := true,
    libraryDependencies ++= Seq(
      "org.powerscala" %% "powerscala-io" % powerScalaVersion
    ),
    assemblyJarName in assembly := "youi-template.jar"
  )
  .dependsOn(app)

lazy val templateJS = template.js.dependsOn(ui)
lazy val templateJVM = template.jvm.dependsOn(serverUndertow, optimizer)

lazy val example = crossProject.in(file("example"))
  .settings(
    name := "youi-example"
  )
  .jsSettings(
    test := (),
    crossTarget in fastOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    crossTarget in fullOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    crossTarget in packageJSDependencies := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    crossTarget in packageMinifiedJSDependencies := baseDirectory.value / ".." / "jvm" / "src" / "main" / "resources" / "app",
    skip in packageJSDependencies := false
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml" % scalaXMLVersion
    )
  )
  .dependsOn(app, template)

lazy val exampleJS = example.js.dependsOn(ui)
lazy val exampleJVM = example.jvm.dependsOn(serverUndertow)