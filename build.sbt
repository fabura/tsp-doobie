name := "scala-test-twelve"

version := "0.1"

scalaVersion := "2.12.8"

ThisBuild / organization := "com.example"

val ZIOVersion = "1.0-RC4"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala" % "0.27.0",
    libraryDependencies += "org.testcontainers" % "postgresql" % "1.11.3" ,
    libraryDependencies += "org.testcontainers" % "clickhouse" % "1.11.3" ,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21", // logger implementation"org.tpolecat" %% "doobie-core"      % "0.7.0",
    libraryDependencies += "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
    libraryDependencies += "org.tpolecat" %% "doobie-core" % "0.7.0",
    libraryDependencies += "org.scalaz" %% "scalaz-zio" % ZIOVersion,
    libraryDependencies += "org.scalaz" %% "scalaz-zio-interop-cats" % ZIOVersion, 
      libraryDependencies += "org.specs2" %% "specs2-core" % "4.6.0" % "test"

  )