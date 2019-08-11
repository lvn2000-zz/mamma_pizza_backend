import Resolvers._
import Dependencies._

lazy val root = (project in file("."))
.settings(
  buildSettings,
  resolvers ++= myResolvers,
  libraryDependencies ++= (commonDeps ++ testDeps)
)

// factor out common settings into a sequence
lazy val buildSettings = Seq(
name := "Pizzeria Backend",
organization := "com.pizzeria.backend",
version := "0.1.0",
scalaVersion := "2.12.8",
fork in Test := true,
fork in run := true,
 mainClass in(Compile, run) := Some("com.pizzeria.backend.Server")
)

// Sub-project specific dependencies
lazy val commonDeps = Seq(
common_library,		
db.postgresJdbc, db.slick, db.slick_hikaricp, db.slick_codegen,
akka.actor, akka.slf4j, akka.alpakka_kafka,
logging.logback,typesafeConfig, fst, cats.core
)

unmanagedBase := baseDirectory.value / "extlibs" 

lazy val testDeps = Seq(testPizza.scalatest, testPizza.scalacheck, akka.testkit)

//https://typelevel.org/cats/
scalacOptions += "-Ypartial-unification"

