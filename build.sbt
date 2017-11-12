lazy val commonSettings = inThisBuild(
  List(
    organization := "com.whisk",
    scalaVersion := "2.12.4",
    version := "0.1.3",
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:implicitConversions"),
    sonatypeProfileName := "com.whisk",
    publishMavenStyle := true,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/whisklabs/hulk")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/whisklabs/hulk"),
        "scm:git:github.com/whisklabs/hulk.git"
      )
    ),
    developers := List(
      Developer(id = "viktortnk",
                name = "Viktor Taranenko",
                email = "viktortnk@gmail.com",
                url = url("https://finelydistributed.io/"))
    ),
    publishTo := Some(Opts.resolver.sonatypeStaging)
  ))

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publish := {}, publishLocal := {}, packagedArtifacts := Map.empty)
  .aggregate(core, testing, circe, tests)

lazy val core = project
  .in(file("hulk-core"))
  .settings(
    name := "hulk-core",
    commonSettings,
    libraryDependencies ++= Seq(
      "com.github.mauricio" %% "postgresql-async" % "0.2.21",
    )
  )

lazy val testing = project
  .in(file("hulk-testing"))
  .settings(
    name := "hulk-testing",
    commonSettings,
    libraryDependencies ++= Seq(
      "com.whisk" %% "docker-testkit-scalatest" % "0.10.0-beta2",
      "org.postgresql" % "postgresql" % "42.1.4"
    )
  )
  .dependsOn(core)

lazy val circe = project
  .in(file("hulk-circe"))
  .settings(
    name := "hulk-circe",
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.9.0-M2",
      "io.circe" %% "circe-parser" % "0.9.0-M2"
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val tests = project
  .in(file("hulk-tests"))
  .settings(
    name := "hulk-tests",
    commonSettings,
    publish := {},
    publishLocal := {},
    packagedArtifacts := Map.empty
  )
  .dependsOn(core % Test, circe % Test, testing % Test)
