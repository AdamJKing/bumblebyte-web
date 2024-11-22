ThisBuild / scalaVersion := "3.4.2"

lazy val root = (project in file("."))
  .settings(
    name := "bumblebyte-web",
    organization := "uk.co.bumblebyte",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "com.lihaoyi" %% "os-lib" % "0.11.3",
      "com.lihaoyi" %% "os-lib-watch" % "0.11.3",
      "org.commonmark" % "commonmark-ext-yaml-front-matter" % "0.24.0"
    )
  )
