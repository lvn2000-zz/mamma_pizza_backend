import sbt._

object Dependencies {

  val common_library = "com.pizzeria.common" %% "pizzeria-common-library" % "0.1.0"
  
  object akka {
    val version = "2.5.14"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    val testkit = "com.typesafe.akka" %% "akka-testkit" % version % "test"
    val alpakka_kafka = "com.typesafe.akka" %% "akka-stream-kafka" % "0.22"
    //		val akka_http_version = "10.1.1"
    //		val http = "com.typesafe.akka" %% "akka-http" % akka_http_version
    //		val http_test_kit = "com.typesafe.akka" %% "akka-http-testkit" % akka_http_version
  }

  object db {
    val slick_version = "3.2.3"
    val postgresJdbc = "org.postgresql" % "postgresql" % "42.2.4"
    val slick = "com.typesafe.slick" %% "slick" % slick_version
    val slick_hikaricp = "com.typesafe.slick" %% "slick-hikaricp" % slick_version
    val slick_codegen = "com.typesafe.slick" %% "slick-codegen" % slick_version
  }

  object logging {
    val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  }

  object testPizza {
    val scalatest = "org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % "test"
    val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
  }

  val typesafeConfig = "com.typesafe" % "config" % "1.3.1"

  val fst = "de.ruedigermoeller" % "fst" % "2.57"

  //val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.25"
  object cats {
    val version = "1.2.0"
    val core = "org.typelevel" %% "cats-core" % version
  }

  //val reactive =  "io.reactivex" %% "rxscala" % "0.26.5"

}
